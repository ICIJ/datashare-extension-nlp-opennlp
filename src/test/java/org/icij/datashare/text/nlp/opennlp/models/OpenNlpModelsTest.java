package org.icij.datashare.text.nlp.opennlp.models;

import opennlp.tools.util.model.ArtifactProvider;
import opennlp.tools.util.model.BaseModel;
import org.icij.datashare.io.RemoteFiles;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.nlp.AbstractModels;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class OpenNlpModelsTest {
    AutoCloseable mocks;
    private final RemoteFiles mockRemoteFiles = mock(RemoteFiles.class);
    @Before
    public void setUp() {
        System.clearProperty(AbstractModels.JVM_PROPERTY_NAME);
        mocks = openMocks(this);
    }
    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void test_download_model() throws Exception {
        final OpenNlpModels model = new OpenNlpModels();

        model.get(Language.ENGLISH);
        verify(mockRemoteFiles).download("dist/models/opennlp/1-5/en", new File(System.getProperty("user.dir")));
        reset(mockRemoteFiles);

        model.get(Language.ENGLISH);
        verify(mockRemoteFiles, never()).download(any(String.class), any(File.class));
    }

    class OpenNlpModels extends org.icij.datashare.text.nlp.opennlp.models.OpenNlpModels {
        OpenNlpModels() { super();}
        @Override
        protected ArtifactProvider createModel(InputStream io) {return mock(BaseModel.class);}
        @Override
        String getModelPath(Language language) { return "unused";}
        @Override
        protected boolean isPresent(Language language) {return false;}
        @Override
        protected RemoteFiles getRemoteFiles() { return mockRemoteFiles;}
    }
}
