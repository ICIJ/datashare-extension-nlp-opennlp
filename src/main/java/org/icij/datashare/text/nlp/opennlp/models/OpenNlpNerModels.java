package org.icij.datashare.text.nlp.opennlp.models;

import static org.icij.datashare.text.Language.ENGLISH;
import static org.icij.datashare.text.Language.FRENCH;
import static org.icij.datashare.text.Language.SPANISH;
import static org.icij.datashare.text.NamedEntity.Category.LOCATION;
import static org.icij.datashare.text.NamedEntity.Category.ORGANIZATION;
import static org.icij.datashare.text.NamedEntity.Category.PERSON;

import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.model.ArtifactProvider;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.NamedEntity;
import java.lang.UnsupportedOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;



public class OpenNlpNerModels extends OpenNlpModels {
    private static volatile OpenNlpNerModels instance;
    private static final Object mutex = new Object();
    private final Map<Language, Map<NamedEntity.Category, String>> modelsFilenames;

    public static OpenNlpNerModels getInstance() {
        OpenNlpNerModels local_instance = instance;
         if (local_instance == null) {
             synchronized(mutex) {
                 local_instance = instance;
                 if (local_instance == null) {
                     instance = new OpenNlpNerModels();
                 }
             }
         }
         return instance;
     }

    private OpenNlpNerModels() {
        super();
        modelsFilenames = Map.of(
            ENGLISH, Map.of(
                PERSON, "en-ner-person.bin",
                ORGANIZATION, "en-ner-organization.bin",
                LOCATION, "en-ner-location.bin"
            ),
            FRENCH, Map.of(
                PERSON, "fr-ner-person.bin",
                ORGANIZATION, "fr-ner-organization.bin",
                LOCATION, "fr-ner-location.bin"
            ),
            SPANISH, Map.of(
                PERSON, "es-ner-person.bin",
                ORGANIZATION, "es-ner-organization.bin",
                LOCATION, "es-ner-location.bin"
            )
        );
    }

    @Override
    protected ArtifactProvider loadModelFile(Language language) throws IOException {
        OpenNlpCompositeModel compositeModels = new OpenNlpCompositeModel(language);
        for (String p: modelsFilenames.get(language).values()) {
            final Path path = getModelsBasePath(language).resolve(p);
            LOGGER.info("loading NER model file " + path);
            try (InputStream modelIS = ClassLoader.getSystemResourceAsStream(path.toString())) {
                compositeModels.add(createModel(modelIS));
            }
        }
        return compositeModels;
    }

    @Override
    ArtifactProvider createModel(InputStream is) throws IOException {
        return new TokenNameFinderModel(is);
    }

    @Override
    String getModelPath(Language language) {
        throw new UnsupportedOperationException();
    }
}
