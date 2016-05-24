package ankush.esplugins;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package org.apache.lucene.search.similarities;

/*
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.util.BytesRef;
*/


import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

import java.lang.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;




public class ModifiedTfidfSimilarity extends Similarity {

  /**
   * Sole constructor. (For invocation by subclass
   * constructors, typically implicit.)
   */
  public ModifiedTfidfSimilarity() {}

    /** Computes a score factor based on a term or phrase's frequency in a
     * document.  This value is multiplied by the {@link #idf(long, long)}
     * factor for each term in the query and these products are then summed to
     * form the initial score for a document.
     *
     * <p>Terms and phrases repeated in a document indicate the topic of the
     * document, so implementations of this method usually return larger values
     * when <code>freq</code> is large, and smaller values when <code>freq</code>
     * is small.
     *
     * @param freq the frequency of a term within a document
     * @return a score factor based on a term's within-document frequency
     */
         public float tf(float freq){
      return (float)Math.sqrt(freq);
    }


    public float idf(long docFreq, long numDocs){
        return (float)Math.log(numDocs/ (docFreq+1)) + 1 ;
    };

    /**
     * Decodes a normalization factor stored in an index.
     *
     * @see #encodeNormValue(float)
     */
    public float decodeNormValue(long norm){
      return (float) norm;
    }

    /** Encodes a normalization factor for storage in an index. */
    public long encodeNormValue(float f){
      return (long)f;
    }


    @Override
    //called at indexing time to compute the norm of a document
    public final long computeNorm(FieldInvertState state) {
        final int numTerms = state.getLength();
        return (long)numTerms;
    }


    public float scorePayload(int doc, int start, int end, BytesRef payload){
      return 1.0f;
    }

    /**
     * Calculate a scoring factor based on the data in the payload.  Implementations
     * are responsible for interpreting what is in the payload.  Lucene makes no assumptions about
     * what is in the byte array.
     *
     * @param doc The docId currently being scored.
     * @param start The start position of the payload
     * @param end The end position of the payload
     * @param payload The payload byte array to be scored
     * @return An implementation dependent float to be used as a scoring factor
     */
    //public abstract float scorePayload(int doc, int start, int end, BytesRef payload);

    @Override
    public final SimWeight computeWeight(float unknown, CollectionStatistics collectionStats, TermStatistics... termStats) {
      final Explanation idf = termStats.length == 1 ? idfExplain(collectionStats, termStats[0]) : idfExplain(collectionStats, termStats);
      return new IDFStats(collectionStats.field(), idf);
    }

    @Override
    public final SimScorer simScorer(SimWeight stats, LeafReaderContext context) throws IOException {
      IDFStats idfstats = (IDFStats) stats;
      return new TFIDFSimScorer(idfstats, context.reader().getNormValues(idfstats.field));
    }

    private final class TFIDFSimScorer extends SimScorer {
      private final IDFStats stats;
      private final float weightValue;
      private final NumericDocValues norms;

      TFIDFSimScorer(IDFStats stats, NumericDocValues norms) throws IOException {
        this.stats = stats;
        this.weightValue = stats.value;
        this.norms = norms;
      }

      @Override
      public float score(int doc, float freq) {
        final float raw = tf(freq) * weightValue; // compute tf(f)*weight

        return norms == null ? raw : raw * decodeNormValue(norms.get(doc));  // normalize for field
      }


      public float sloppyFreq(int distance){
          return 1 / (1 + distance);
      }

      @Override
      public float computeSlopFactor(int distance) {
        return sloppyFreq(distance);
      }


      @Override
      public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
        return scorePayload(doc, start, end, payload);
      }

      @Override
      public Explanation explain(int doc, Explanation freq) {
        return explainScore(doc, freq, stats, norms);
      }
    }

    /** Collection statistics for the TF-IDF model. The only statistic of interest
     * to this model is idf. */
    private static class IDFStats extends SimWeight {
      private final String field;
      /** The idf and its explanation */
      private final Explanation idf;
      private float queryNorm;
      private float boost;
      private float queryWeight;
      private float value;

      public IDFStats(String field, Explanation idf) {
        // TODO: Validate?
        this.field = field;
        this.idf = idf;
        normalize(1f, 1f);
      }

      @Override
      public float getValueForNormalization() {
        // TODO: (sorta LUCENE-1907) make non-static class and expose this squaring via a nice method to subclasses?
        return queryWeight * queryWeight;  // sum of squared weights
      }

      @Override
      public void normalize(float queryNorm, float boost) {
        this.boost = boost;
        this.queryNorm = queryNorm;
        queryWeight = queryNorm * boost * idf.getValue();
        value = queryWeight * idf.getValue();         // idf for document
      }
    }

    private Explanation explainQuery(IDFStats stats) {
      List<Explanation> subs = new ArrayList<>();

      Explanation boostExpl = Explanation.match(stats.boost, "boost");
      if (stats.boost != 1.0f)
        subs.add(boostExpl);
      subs.add(stats.idf);

      Explanation queryNormExpl = Explanation.match(stats.queryNorm,"queryNorm");
      subs.add(queryNormExpl);

      return Explanation.match(
          boostExpl.getValue() * stats.idf.getValue() * queryNormExpl.getValue(),
          "queryWeight, product of:", subs);
    }

    private Explanation explainField(int doc, Explanation freq, IDFStats stats, NumericDocValues norms) {
      Explanation tfExplanation = Explanation.match(tf(freq.getValue()), "tf(freq="+freq.getValue()+"), with freq of:", freq);
      Explanation fieldNormExpl = Explanation.match(
          norms != null ? decodeNormValue(norms.get(doc)) : 1.0f,
          "fieldNorm(doc=" + doc + ")");

      return Explanation.match(
          tfExplanation.getValue() * stats.idf.getValue() * fieldNormExpl.getValue(),
          "fieldWeight in " + doc + ", product of:",
          tfExplanation, stats.idf, fieldNormExpl);
    }

    private Explanation explainScore(int doc, Explanation freq, IDFStats stats, NumericDocValues norms) {
      Explanation queryExpl = explainQuery(stats);
      Explanation fieldExpl = explainField(doc, freq, stats, norms);
      if (queryExpl.getValue() == 1f) {
        return fieldExpl;
      }
      return Explanation.match(
          queryExpl.getValue() * fieldExpl.getValue(),
          "score(doc="+doc+",freq="+freq.getValue()+"), product of:",
          queryExpl, fieldExpl);
    }


    public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats) {
      final long df = termStats.docFreq();
      final long docCount = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();
      final float idf = idf(df, docCount);
      return Explanation.match(idf, "idf(docFreq=" + df + ", docCount=" + docCount + ")");
    }

    /**
     * Computes a score factor for a phrase.
     *
     * <p>
     * The default implementation sums the idf factor for
     * each term in the phrase.
     *
     * @param collectionStats collection-level statistics
     * @param termStats term-level statistics for the terms in the phrase
     * @return an Explain object that includes both an idf
     *         score factor for the phrase and an explanation
     *         for each term.
     */
    public Explanation idfExplain(CollectionStatistics collectionStats, TermStatistics termStats[]) {
      final long docCount = collectionStats.docCount() == -1 ? collectionStats.maxDoc() : collectionStats.docCount();
      float idf = 0.0f;
      List<Explanation> subs = new ArrayList<>();
      for (final TermStatistics stat : termStats ) {
        final long df = stat.docFreq();
        final float termIdf = idf(df, docCount);
        subs.add(Explanation.match(termIdf, "idf(docFreq=" + df + ", docCount=" + docCount + ")"));
        idf += termIdf;
      }
      return Explanation.match(idf, "idf(), sum of:", subs);
    }

}
