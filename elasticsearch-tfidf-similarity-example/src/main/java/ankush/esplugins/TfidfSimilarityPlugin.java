package ankush.esplugins;

import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.plugins.Plugin;

public class TfidfSimilarityPlugin extends Plugin {

    @Override
    public String name() {
        return "tfidf-similarity-plugin";
        
    }

    @Override
    public String description() {
        return "Tfidf Similarity Plugin";
    }

    public void onModule(final SimilarityModule module) {
        module.addSimilarity("tfidfsimilarity", TfidfSimilarityProvider.class);
    }
}
