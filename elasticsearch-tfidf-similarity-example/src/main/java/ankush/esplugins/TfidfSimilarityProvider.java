package ankush.esplugins;

import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.similarity.*;

public class TfidfSimilarityProvider extends AbstractSimilarityProvider {

    private ModifiedTfidfSimilarity similarity;

    @Inject
    public TfidfSimilarityProvider(@Assisted String name, @Assisted Settings settings) {
        super(name);
        this.similarity = new ModifiedTfidfSimilarity();
    }

    @Override
    public Similarity get() {
        return similarity;
    }


}
