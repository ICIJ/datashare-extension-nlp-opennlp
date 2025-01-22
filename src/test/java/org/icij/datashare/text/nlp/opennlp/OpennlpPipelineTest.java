package org.icij.datashare.text.nlp.opennlp;

import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.text.Language;
import org.icij.datashare.text.nlp.AbstractModels;
import org.junit.Test;

import java.util.Properties;

import static org.fest.assertions.Assertions.assertThat;

public class OpennlpPipelineTest {

    @Test
    public void test_initialize() throws InterruptedException {
        AbstractModels.syncModels(false);
        Properties props = new Properties();
        OpennlpPipeline openNlpPipeline = new OpennlpPipeline(new PropertiesProvider(props));
        openNlpPipeline.initialize(Language.ENGLISH);

        assertThat(openNlpPipeline.sentencer.keySet()).contains(Language.ENGLISH);
        assertThat(openNlpPipeline.tokenizer.keySet()).contains(Language.ENGLISH);
        assertThat(openNlpPipeline.nerFinder.keySet()).contains(Language.ENGLISH);
    }
}