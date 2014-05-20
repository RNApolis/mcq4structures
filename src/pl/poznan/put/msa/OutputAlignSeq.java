package pl.poznan.put.msa;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.biojava3.alignment.NeedlemanWunsch;
import org.biojava3.alignment.template.AbstractPairwiseSequenceAligner;
import org.biojava3.alignment.template.AlignedSequence;
import org.biojava3.alignment.template.SequencePair;
import org.biojava3.core.sequence.template.Compound;
import org.biojava3.core.sequence.template.Sequence;

import pl.poznan.put.interfaces.Exportable;

public class OutputAlignSeq implements Exportable {
    private static void generateLine(StringBuilder builder, char[] chars, int i) {
        int j = i;
        while (j < i + 60 && j < chars.length) {
            builder.append(chars[j]);
            j++;
        }
        builder.append("  ");
        builder.append(Integer.toString(j));
        builder.append('\n');
    }

    public int gaps;
    public int length;
    public int maxScore;
    public int minScore;
    public int score;
    public double similarity;
    private String consensus;
    private String description;
    private boolean isGlobal;
    private AlignedSequence<Sequence<Compound>, Compound> query;
    private AlignedSequence<Sequence<Compound>, Compound> target;

    public OutputAlignSeq(
            AbstractPairwiseSequenceAligner<Sequence<Compound>, Compound> aligner,
            String description) {
        isGlobal = aligner instanceof NeedlemanWunsch<?, ?>;
        this.description = description;

        SequencePair<Sequence<Compound>, Compound> pair = aligner.getPair();
        query = pair.getQuery();
        target = pair.getTarget();

        String queryStr = query.toString();
        String targetStr = target.toString();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < query.getLength(); i++) {
            char compoundQuery = queryStr.charAt(i);
            char compoundTarget = targetStr.charAt(i);
            if (compoundQuery == compoundTarget) {
                builder.append(compoundQuery);
            } else {
                builder.append(' ');
            }
        }
        consensus = builder.toString();

        score = aligner.getScore();
        minScore = aligner.getMinScore();
        maxScore = aligner.getMaxScore();
        similarity = aligner.getSimilarity();
        length = query.getLength();

        gaps = 0;
        for (AlignedSequence<Sequence<Compound>, Compound> seq : pair.getAlignedSequences()) {
            gaps += StringUtils.countMatches(seq.getSequenceAsString(), "-");
        }
    }

    @Override
    public void export(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            writer.write(isGlobal ? "Global" : "Local");
            writer.write(" sequence alignment: ");
            writer.write(description);
            writer.write("\n\n");
            writer.write(toString());
        }
    }

    @Override
    public File suggestName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
        String filename = sdf.format(new Date());
        filename += isGlobal ? "-GSA-" : "-LSA-";
        filename += description.replace(", ", "-");
        filename += ".txt";
        return new File(filename);
    }

    @Override
    public String toString() {
        char[] charsQuery = query.toString().toCharArray();
        char[] charsTarget = target.toString().toCharArray();
        char[] charsConsensus = consensus.toCharArray();

        assert charsQuery.length == charsTarget.length;
        assert charsQuery.length == charsConsensus.length;

        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Score: %d (min: %d, max: %d)%n"
                + "Similarity: %.0f%%%nGaps: %d/%d (%.0f%%)%n%n", score,
                minScore, maxScore, 100.0 * similarity, gaps, length, 100.0
                        * gaps / length));

        for (int i = 0; i < charsQuery.length; i += 60) {
            builder.append(String.format("Query  %-5d", i + 1));
            OutputAlignSeq.generateLine(builder, charsQuery, i);

            builder.append("            ");
            OutputAlignSeq.generateLine(builder, charsConsensus, i);

            builder.append(String.format("Sbjct  %-5d", i + 1));
            OutputAlignSeq.generateLine(builder, charsTarget, i);

            builder.append('\n');
        }

        return builder.toString();
    }
}
