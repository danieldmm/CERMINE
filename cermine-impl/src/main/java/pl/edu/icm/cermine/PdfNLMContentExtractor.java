/**
 * This file is part of CERMINE project.
 * Copyright (c) 2011-2013 ICM-UW
 *
 * CERMINE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CERMINE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with CERMINE. If not, see <http://www.gnu.org/licenses/>.
 */

package pl.edu.icm.cermine;

import com.google.common.collect.Lists;
import java.io.*;
import java.util.Collection;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.exception.TransformationException;
import pl.edu.icm.cermine.structure.model.BxDocument;
import pl.edu.icm.cermine.structure.transformers.BxDocumentToTrueVizWriter;

/**
 * NLM-based content extractor from PDF files.
 *
 * @author Dominika Tkaczyk
 */
public class PdfNLMContentExtractor {

    private ComponentConfiguration conf;
    
    private boolean extractMetadata = true;
    
    private boolean extractReferences = true;
    
    private boolean extractText = true;
    
    public static int THREADS_NUMBER = 3;

    
    public PdfNLMContentExtractor() throws AnalysisException {
        conf = new ComponentConfiguration();
    }

    /**
     * Extracts content from PDF file and stores it in NLM format.
     * 
     * @param stream input stream
     * @return extracted content in NLM format
     * @throws AnalysisException 
     */
    public Element extractContent(InputStream stream) throws AnalysisException {
        BxDocument doc = ExtractionUtils.extractStructure(conf, stream);
        return extractContent(doc);
    }

    /**
     * Extracts content from a BxDocument and stores it in NLM format.
     * 
     * @param document document's structure
     * @return extracted content in NLM format
     * @throws AnalysisException 
     */
    public Element extractContent(BxDocument document) throws AnalysisException {
        Element content = new Element("article");
        
        Element metadata = new Element("front");
        if (extractMetadata) {
            Element meta = ExtractionUtils.extractMetadataAsNLM(conf, document);
            metadata = (Element) meta.getChild("front").clone();
        }
        content.addContent(metadata);
        
        Element text = new Element("body");
        if (extractText) {
            text = ExtractionUtils.extractTextAsNLM(conf, document);
        }
        content.addContent(text);
        
        Element back = new Element("back");
        Element refList = new Element("ref-list");
        if (extractReferences) {
            Element[] references = ExtractionUtils.extractReferencesAsNLM(conf, document);
            for (int i = 0; i < references.length; i++) {
                Element ref = references[i];
                Element r = new Element("ref");
                r.setAttribute("id", String.valueOf(i+1));
                r.addContent(ref);
                refList.addContent(r);
            }
        }
        back.addContent(refList);
        content.addContent(back);

        return content;
    }

    public ComponentConfiguration getConf() {
        return conf;
    }

    public void setConf(ComponentConfiguration conf) {
        this.conf = conf;
    }
    
    public boolean isExtractMetadata() {
        return extractMetadata;
    }

    public void setExtractMetadata(boolean extractMetadata) {
        this.extractMetadata = extractMetadata;
    }

    public boolean isExtractReferences() {
        return extractReferences;
    }

    public void setExtractReferences(boolean extractReferences) {
        this.extractReferences = extractReferences;
    }

    public boolean isExtractText() {
        return extractText;
    }

    public void setExtractText(boolean extractText) {
        this.extractText = extractText;
    }

    public static void main(String[] args) throws ParseException, IOException {
        CommandLineOptionsParser parser = new CommandLineOptionsParser();
        if (!parser.parse(args)) {
            System.err.println(
                    "Usage: PdfNLMContentExtractor -path <path> [optional parameters]\n\n"
                  + "Tool for extracting metadata and content from PDF files.\n\n"
                  + "Arguments:\n"
                  + "  -path <path>              path to a PDF file or directory containing PDF files\n"
                  + "  -ext <extension>          (optional) the extension of the resulting metadata file;\n"
                  + "                            default: \"cermxml\"; used only if passed path is a directory\n"
                  + "  -modelmeta <path>         (optional) the path to the metadata classifier model file\n"
                  + "  -modelinit <path>         (optional) the path to the initial classifier model file\n"
                  + "  -str                      whether to store structure (TrueViz) files as well;\n"
                  + "                            used only if passed path is a directory\n"
                  + "  -strext <extension>       (optional) the extension of the structure (TrueViz) file;\n"
                  + "                            default: \"cxml\"; used only if passed path is a directory\n"
                  + "  -threads <num>            number of threads for parallel processing\n");
            System.exit(1);
        }
        
        String path = parser.getPath();
        String extension = parser.getNLMExtension();
        boolean extractStr = parser.extractStructure();
        String strExtension = parser.getBxExtension();
        PdfNLMContentExtractor.THREADS_NUMBER = parser.getThreadsNumber();
 
        File file = new File(path);
        if (file.isFile()) {
            try {
                PdfNLMContentExtractor extractor = new PdfNLMContentExtractor();
                parser.updateMetadataModel(extractor.getConf());
                parser.updateInitialModel(extractor.getConf());
                InputStream in = new FileInputStream(file);
                Element result = extractor.extractContent(in);
                XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                System.out.println(outputter.outputString(result));
            } catch (AnalysisException ex) {
                ex.printStackTrace();
            }
        } else {
        
            Collection<File> files = FileUtils.listFiles(file, new String[]{"pdf"}, true);
    
            int i = 0;
            for (File pdf : files) {
                File xmlF = new File(pdf.getPath().replaceAll("pdf$", extension));
                if (xmlF.exists()) {
                    i++;
                    continue;
                }
 
                long start = System.currentTimeMillis();
                float elapsed = 0;
            
                System.out.println(pdf.getPath());
 
                try {
                    PdfNLMContentExtractor extractor = new PdfNLMContentExtractor();
                    parser.updateMetadataModel(extractor.getConf());
                    parser.updateInitialModel(extractor.getConf());

                    InputStream in = new FileInputStream(pdf);
                    BxDocument doc = ExtractionUtils.extractStructure(extractor.getConf(), in);
                    Element result = extractor.extractContent(doc);

                    long end = System.currentTimeMillis();
                    elapsed = (end - start) / 1000F;
            
                    XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
                    if (!xmlF.createNewFile()) {
                        System.out.println("Cannot create new file!");
                    }
                    FileUtils.writeStringToFile(xmlF, outputter.outputString(result));            
                
                    if (extractStr) {
                        BxDocumentToTrueVizWriter writer = new BxDocumentToTrueVizWriter();
                        File strF = new File(pdf.getPath().replaceAll("pdf$", strExtension));
                        writer.write(new FileWriter(strF), Lists.newArrayList(doc));
                    }
                } catch (AnalysisException ex) {
                   ex.printStackTrace();
                } catch (TransformationException ex) {
                   ex.printStackTrace();
                }
                
                i++;
                int percentage = i*100/files.size();
                if (elapsed == 0) {
                    elapsed = (System.currentTimeMillis() - start) / 1000F;
                }
                System.out.println("Extraction time: " + Math.round(elapsed) + "s");
                System.out.println(percentage + "% done (" + i +" out of " + files.size() + ")");
                System.out.println("");
            }
        }
    }

}
