package org.icij.datashare.text.nlp.opennlp.models;

import opennlp.tools.util.model.ArtifactProvider;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.nlp.AbstractModels;
import org.icij.datashare.text.nlp.Pipeline;

import java.io.IOException;
import java.io.InputStream;

public abstract class OpenNlpModels extends AbstractModels<ArtifactProvider> {
    static final String VERSION = "1.5";

    OpenNlpModels() { super(Pipeline.Type.OPENNLP);}

    @Override
    protected String getVersion() { return VERSION;}

    @Override
    protected ArtifactProvider loadModelFile(Language language) throws IOException {
        final String modelPath = getModelPath(language);
        LOGGER.info("loading model file " + modelPath);
        try (InputStream modelIS = ClassLoader.getSystemResourceAsStream(modelPath)) {
            return createModel(modelIS);
        }
     }

    abstract ArtifactProvider createModel (InputStream is) throws IOException;
    abstract String getModelPath (Language languate);

}
