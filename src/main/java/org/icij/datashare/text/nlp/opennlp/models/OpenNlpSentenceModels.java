package org.icij.datashare.text.nlp.opennlp.models;

import static org.icij.datashare.text.Language.ENGLISH;
import static org.icij.datashare.text.Language.FRENCH;
import static org.icij.datashare.text.Language.SPANISH;

import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.model.ArtifactProvider;
import org.icij.datashare.text.Language;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;



public class OpenNlpSentenceModels extends OpenNlpModels {
    private static volatile OpenNlpSentenceModels instance;
    private static final Object mutex = new Object();

    private final Map<Language, String> modelFilenames;

    public static OpenNlpSentenceModels getInstance() {
        OpenNlpSentenceModels local_instance = instance; // avoid accessing volatile field
        if (local_instance == null) {
            synchronized (mutex) {
                local_instance = instance;
                if (local_instance == null) {
                    instance = new OpenNlpSentenceModels();
                }
            }
        }
        return instance;
    }

    private OpenNlpSentenceModels() {
        modelFilenames = Map.of(
            ENGLISH, "en-sent.bin",
            SPANISH, "en-sent.bin",
            FRENCH, "fr-sent.bin"
        );
    }

    protected ArtifactProvider createModel(InputStream content) throws IOException {
        return new SentenceModel(content);
    }

    protected String getModelPath(Language language) {
        return getModelsBasePath(language).resolve(modelFilenames.get(language)).toString();
    }
}
