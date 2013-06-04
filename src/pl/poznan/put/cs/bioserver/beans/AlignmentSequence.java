package pl.poznan.put.cs.bioserver.beans;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.biojava.bio.structure.Chain;
import org.biojava3.alignment.template.AlignedSequence;
import org.biojava3.alignment.template.Profile;
import org.biojava3.core.sequence.template.Compound;
import org.biojava3.core.sequence.template.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.poznan.put.cs.bioserver.alignment.AlignerSequence;
import pl.poznan.put.cs.bioserver.helper.Exportable;
import pl.poznan.put.cs.bioserver.helper.Helper;
import pl.poznan.put.cs.bioserver.helper.StructureManager;

@XmlRootElement
public class AlignmentSequence extends XMLSerializable implements Exportable {
    private static final long serialVersionUID = -819554091819458384L;
    private static final Logger LOGGER = LoggerFactory.getLogger(AlignmentSequence.class);

    // /////////////////////////////////////////////////////////////////////////
    // static "constructors"
    public static AlignmentSequence newInstance(List<Chain> chains, boolean isGlobal) {
        Profile<Sequence<Compound>, Compound> profile = AlignerSequence.align(chains, isGlobal);
        return AlignmentSequence.newInstance(profile, chains, isGlobal);
    }

    public static AlignmentSequence newInstance(Profile<Sequence<Compound>, Compound> profile,
            List<Chain> chains, boolean isGlobal) {
        /*
         * get name of every structure and chain
         */
        List<String> names = new ArrayList<>();
        for (Chain chain : chains) {
            names.add(StructureManager.getName(chain.getParent()) + "." + chain.getChainID());
        }

        /*
         * prepare a title (names separated with comma)
         */
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            builder.append(name);
            builder.append(", ");
        }
        builder.delete(builder.length() - 2, builder.length());
        String title = builder.toString();

        /*
         * convert every sequence into an array of characters
         */
        List<AlignedSequence<Sequence<Compound>, Compound>> list = profile.getAlignedSequences();
        char[][] sequences = new char[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            sequences[i] = list.get(i).toString().toCharArray();
            assert sequences[i].length == sequences[0].length;
        }

        /*
         * format alignment to clustalw
         */
        builder = new StringBuilder();
        for (int i = 0; i < sequences[0].length; i += 60) {
            char[][] copy = new char[list.size()][];
            for (int j = 0; j < list.size(); j++) {
                copy[j] = Arrays
                        .copyOfRange(sequences[j], i, Math.min(i + 60, sequences[j].length));
                String name = names.get(j).substring(0, Math.min(names.get(j).length(), 11));
                builder.append(String.format("%-12s", name));
                builder.append(copy[j]);
                builder.append('\n');
            }
            builder.append("            ");
            for (int k = 0; k < copy[0].length; k++) {
                boolean flag = true;
                for (int j = 0; j < list.size(); j++) {
                    if (copy[j][k] != copy[0][k]) {
                        flag = false;
                        break;
                    }
                }
                builder.append(flag ? '*' : ' ');
            }
            builder.append("\n\n");
        }

        /*
         * construct a final object
         */
        AlignmentSequence result = new AlignmentSequence();
        result.setAlignment(builder.toString());
        result.setGlobal(isGlobal);
        result.setTitle(title);
        return result;
    }

    boolean isGlobal;
    String alignment;
    String title;

    // /////////////////////////////////////////////////////////////////////////
    // other methods, implementation of interfaces
    @Override
    public void export(File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            writer.write(isGlobal ? "Global" : "Local");
            writer.write(" sequence alignment: ");
            writer.write(title);
            writer.write("\n\n");
            writer.write(alignment);
        } catch (UnsupportedEncodingException e) {
            AlignmentSequence.LOGGER.error("Failed to export sequence alignment", e);
            throw new IOException(e);
        }
    }

    public String getAlignment() {
        return alignment;
    }

    public String getTitle() {
        return title;
    }

    public boolean isGlobal() {
        return isGlobal;
    }

    @XmlElement
    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    @XmlElement
    public void setGlobal(boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    @XmlElement
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public File suggestName() {
        StringBuilder filename = new StringBuilder();
        filename.append(Helper.getExportPrefix());
        filename.append(isGlobal ? "-GSA-" : "-LSA-");
        filename.append(title.replace(", ", "-"));
        filename.append(".txt");
        return new File(filename.toString());
    }
}
