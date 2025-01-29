package org.icij.datashare.text.nlp.opennlp;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import opennlp.tools.util.model.ArtifactProvider;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.text.Document;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.NamedEntity;
import org.icij.datashare.text.nlp.AbstractPipeline;
import org.icij.datashare.text.nlp.Annotations;
import org.icij.datashare.text.nlp.opennlp.models.OpenNlpCompositeModel;
import org.icij.datashare.text.nlp.opennlp.models.OpenNlpNerModels;
import org.icij.datashare.text.nlp.opennlp.models.OpenNlpSentenceModels;
import org.icij.datashare.text.nlp.opennlp.models.OpenNlpTokenModels;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static opennlp.tools.util.Span.spansToStrings;
import static org.icij.datashare.text.Language.ENGLISH;
import static org.icij.datashare.text.Language.FRENCH;
import static org.icij.datashare.text.Language.SPANISH;
import static org.icij.datashare.text.NamedEntity.allFrom;


public final class OpennlpPipeline extends AbstractPipeline {

    Map<Language, SentenceDetector> sentencer;
    Map<Language, Tokenizer> tokenizer;
    Map<Language, List<NameFinderME>> nerFinder;

    public OpennlpPipeline(final PropertiesProvider propertiesProvider) {
        super(propertiesProvider.getProperties());
        sentencer = new HashMap<>();
        tokenizer = new HashMap<>();
        nerFinder = new HashMap<>();
    }

    @Override
    public boolean initialize(Language language) throws InterruptedException {
        if (!super.initialize(language)) {
            return false;
        }
        Map<PipelineComponent, Function<Language, Boolean>> annotatorLoader = Map.of(
            PipelineComponent.TOKENIZER, logIfInterrupted(OpennlpPipeline.this::loadTokenizer),
            PipelineComponent.SENTENCER, logIfInterrupted(OpennlpPipeline.this::loadSentenceDetector),
            PipelineComponent.NER, logIfInterrupted(OpennlpPipeline.this::loadNameFinder)
        );
        Arrays.stream(PipelineComponent.values()).forEach(component -> annotatorLoader.get(component).apply(language));
        return true;
    }

    @Override
    public List<NamedEntity> process(Document doc) {
        return process(doc, doc.getContentTextLength(), 0);
    }

    @Override
    public List<NamedEntity> process(Document doc, int contentLength, int contentOffset) {
        Annotations annotations = new Annotations(doc.getId(), getType(), doc.getLanguage());

        // Split input into sentences
        String text = doc.getContent().substring(contentOffset, contentOffset + contentLength);
        Span[] sentenceSpans = sentences(text, doc.getLanguage());
        for (Span sentenceSpan : sentenceSpans) {
            // Feed annotations
            int sentenceOffsetBegin = sentenceSpan.getStart();

            // Tokenize sentence
            String sentence = sentenceSpan.getCoveredText(doc.getContent()).toString();
            Span[] sentenceTokenSpans = tokenize(sentence, doc.getLanguage());
            String[] sentenceTokens  = spansToStrings(sentenceTokenSpans, sentence);

            // NER on sentence
            for (NameFinderME nameFinderME : nerFinder.get(doc.getLanguage())) {
                Span[] nerSpans = nameFinderME.find(sentenceTokens);
                for (Span nerSpan : nerSpans) {
                    int nerStart = contentOffset + sentenceOffsetBegin + sentenceTokenSpans[nerSpan.getStart()].getStart();
                    int nerEnd   = contentOffset + sentenceOffsetBegin + sentenceTokenSpans[nerSpan.getEnd()-1].getEnd();
                    annotations.add(nerStart, nerEnd, NamedEntity.Category.parse(nerSpan.getType()));
                }
                // Make sure to clear the data after each doc !
                nameFinderME.clearAdaptiveData();
            }
        }
        return allFrom(doc.getContent(), annotations);
    }

    @Override
    public void terminate(Language language) throws InterruptedException {
        super.terminate(language);

        if (nerFinder.containsKey(language)) {
            nerFinder.get(language).forEach(NameFinderME::clearAdaptiveData);
        }

        // (Don't) keep models in memory
        if ( ! caching) {
            sentencer.remove(language);
            tokenizer.remove(language);
            nerFinder.remove(language);
        }
    }

    @FunctionalInterface
    public interface Interruptible<P, E extends Throwable> {
        void apply(P t) throws E;
    }

    public Function<Language, Boolean> logIfInterrupted(Interruptible<Language, InterruptedException> fun) {
        return val -> {
            try {
                fun.apply(val);
                return true;
            } catch (InterruptedException e) {
                LOGGER.error("interrupted", e);
                return false;
            }
        };
    }

    private void loadTokenizer(Language language) throws InterruptedException {
        if (tokenizer.containsKey(language))
            return;
        ArtifactProvider model = OpenNlpTokenModels.getInstance().get(language);
        tokenizer.put(language, new TokenizerME((TokenizerModel) model));
    }

    private void loadSentenceDetector(Language language) throws InterruptedException {
        if (sentencer.containsKey(language))
            return;
        ArtifactProvider model = OpenNlpSentenceModels.getInstance().get(language);
        sentencer.put(language, new SentenceDetectorME((SentenceModel) model));
    }

    private void loadNameFinder(Language language) throws InterruptedException {
        OpenNlpCompositeModel nerModels = (OpenNlpCompositeModel) OpenNlpNerModels.getInstance().get(language);
        final Stream<NameFinderME> nameFinderMEStream = nerModels.models.stream().map(m -> new NameFinderME((TokenNameFinderModel) m));
        nerFinder.put(language, nameFinderMEStream.toList());
    }

    @Override
    public Set<Language> supportedLanguages() {
        return Set.of(ENGLISH, FRENCH, SPANISH);
    }

    private Span[] sentences(String input, Language language) {
        if (!sentencer.containsKey(language))
            return new Span[0];
        return sentencer.get(language).sentPosDetect(input);
    }

    private Span[] tokenize(String input, Language language) {
        if (!tokenizer.containsKey(language))
            return new Span[0];
        return tokenizer.get(language).tokenizePos(input);
    }
}

