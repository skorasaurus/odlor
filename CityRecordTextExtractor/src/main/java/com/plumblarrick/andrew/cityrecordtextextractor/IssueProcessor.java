/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.plumblarrick.andrew.cityrecordtextextractor;

import com.plumblarrick.andrew.cityrecordtextextractor.IssueModel.Page;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author calarrick
 */
public class IssueProcessor {

    String startPdfFileName;
    String firstTextFileName;
    String sortedColumnsFileName;

    /**
     * Instantiates an IsssueExtractorPositional object that handles interaction
     * with the main PDFBox API to parse text from one pdf document into a text
     * file. Returns an indication of success or failure, as a String.
     *
     * @param fileName The name of the starting document, e.g. "test.pdf"
     * @param outFileName The name of the text file to generate, e.g. "test.txt"
     * @return String Will either confirm successful extraction to "example.txt"
     * or indicate that "example.pdf" was not extracted.
     */
    
    
    
    public String extractIssue(String fileName, String outFileName) {

        startPdfFileName = fileName;
        firstTextFileName = outFileName;
        IssueExtractorPositional ex = new IssueExtractorPositional();

        try {
            ex.extractToFile(startPdfFileName, firstTextFileName);
            return "Extracted " + startPdfFileName + " to " + firstTextFileName;
        } catch (IOException io) {
            return "IO Exception: could not extract " + startPdfFileName;

        }
    }


    /**
     * Allows a parsed text file to be provided to the method directly. Simply
     * adds that parameter to the class-level variables and calls the main
     * readLinesToPagees() method. For use if we need to separate "line reading"
     * ordering from text extraction.
     *
     * @param fileName
     * @throws FileNotFoundException
     * @throws IOException
     * @return IssueModel
     */
    public IssueModel readLinesToPages(String fileName) throws
            FileNotFoundException,
            IOException {

        firstTextFileName = fileName;
        IssueModel currIssue = readLinesToPages();
        return currIssue;
    }


    /**
     * Reads the text file generated by the text stripper that is wrapped by the
     * extractIssue method call and models the original document as a group of
     * (corrected) pages and columns.
     *
     * @return IssueModel Object representation of the PDF contents divided into
     * pages and columns with extraction of page numbering and fixes to ordering
     * @throws FileNotFoundException
     * @throws IOException
     */
    public IssueModel readLinesToPages() throws FileNotFoundException,
            IOException {

        BufferedReader textIn = new BufferedReader(new FileReader(
                firstTextFileName), 100000);
        IssueModel currIssue = new IssueModel();
        String issueName;
        Page page = null;
        int pagesAdded = 0;
        int pageLineCounter = 0;

        String currLine = textIn.readLine();

        if (currLine.substring(0, 12).equals("Source file: ")) {
            issueName = currLine.substring(13);
        } else {
            issueName = currLine;
        }
        currIssue.setIssueID(issueName);

        while ((currLine = textIn.readLine()) != null) {

            //currLine = textIn.readLine();
            if (currLine.equals("") || currLine.equals("\n")) {
                continue;
            }

            if ((currLine.length() >= 12) && currLine.substring(0, 12).equals(
                    "[Start Page ")) {
                pagesAdded++;
                page = currIssue.addPage(pagesAdded);
                pageLineCounter = 0;
                page.setCountedPageNum(pagesAdded);
                System.out.println(pagesAdded);

            } else if (page != null) {
                pageLineCounter = page.addLine(currLine);
                System.out.println(pageLineCounter);

            } else {
                page = currIssue.addPage(pagesAdded + 1);
                page.addLine("Page missing correct starting flag.");
                pageLineCounter = page.addLine(currLine);

            }

        }
        currIssue = columnSortIssue(currIssue);
        return currIssue;
    }

    protected IssueModel columnSortIssue(IssueModel issue) {

        List<Page> pages = issue.getPages();
        //replace this w iteration functionality on model

        for (Page page : pages) {
            page = processPage(page);
        }

        return issue;

    }
    //need something topull sinfle col areas out for ordered reintegration


    //calls a page processing method for each known page
    //layout in the set of PDF docs.
    //Ultimately is likely a better and more configurable
    //way to set up this logic.
    protected Page processPage(Page page) {

        //as above

        switch (page.getCountedPageNum()) {

            case 0:
                break;
            case 1:
                break;
            case 2:

                page = processCRDirectoryOfOfficials(page);

                break;
            case 3:
            //falls through for now. will want something here to exclude header material

            default:
                //three column body begins
                //pull out into sep methods by page type
                //poss even use page subclasses to 
                //handle behavior through the model 
                //instead of here
                page = processCRMainBodyPage(page);

        }
        return page;
    }


    protected Page processCRMainBodyPage(Page page) {

        List<String> lines = page.getPageContents();
        int numColsOnLine = 0;
        int lineCounter = 0;
        String pNum = "";

        String[] measureAndText;
        String text = "";
        int xAxisStart = 0;

        boolean indexFlag = false;


        Pattern firstLineIssuePagination = Pattern.compile(
                "[0-9]{1,3}\\t([0-9]{1,3})\\s*");
        Matcher pageMatcher = firstLineIssuePagination.matcher("");

        Pattern colBreak = Pattern.compile("\\|");

        StringBuilder columnOne = new StringBuilder();
        StringBuilder columnTwo = new StringBuilder();
        StringBuilder columnThree = new StringBuilder();
        StringBuilder strays = new StringBuilder();
        strays.append("[Couldn't place these:] \n");
        StringBuilder indexEntries = new StringBuilder();

        for (String line : lines) {

            String[] sections = colBreak.split(line);
            numColsOnLine = sections.length;
            lineCounter++;
            //remember this means lineCounter variable is one greater than 
            //the list index through all following logic

            int columnOneLine = 74;
            int columnTwoLine = 230;
            int columnThreeLine = 380;

            if (line.equals("") || line.equals("\n") || line.equals(
                    " ")) {
                continue;
            }

            if (lineCounter == 1) {

                pageMatcher.reset(sections[0]);

                if (sections.length == 3 && pageMatcher.matches()) {

                    pNum = pageMatcher.group(1);
                    page.setPageNum(Integer.parseInt(pNum));

                } else if (sections.length == 3) {

                    pageMatcher.reset(sections[2]);
                    if (pageMatcher.matches()) {
                        pNum = pageMatcher.group(1);
                    }
                    page.setPageNum(Integer.parseInt(pNum));

                }

                page.setHeader(line);

            } else if (lineCounter == lines.size()) {

                pageMatcher.reset(sections[0]);

                if (sections.length == 1 && pageMatcher.matches()) {

                    pNum = pageMatcher.group(1);
                    page.setIndexPageNum(Integer.parseInt(pNum));


                }
                page.setFooter(line);


            } //index processing -- this now renders up to first several lines
            //of index pages incorrectly as 'column one' rather than index. 
            //need a fix but not sure how to do it without carrying an index 
            //flag from page to page. 
            //To be addressed after further validation that we are getting
            //in-order-ness right
            else if (sections.length == 1 && sections[0].endsWith("Index")) {
                indexFlag = true;
                String[] indexContent = sections[0].split("\t");
                indexEntries.append(indexContent[1]);
                indexEntries.append("\n");
            } else if (sections.length >= 25) {
                //this will be an Index line
                indexFlag = true;
                String[] indexContent = sections[0].split("\t");
                String[] indexedPageNum = sections[sections.length - 1].split(
                        "\t");
                if (indexContent.length >= 2) {
                    indexEntries
                            .append(indexContent[1] + " :\t" + indexedPageNum[1]);
                }

                indexEntries.append("\n");


            } else if (indexFlag) {
                //since need to capture parts of multi-line entries that won't match the above
                String[] indexContent = sections[0].split("\t");
                if (indexContent.length >= 2) {
                    indexEntries.append(indexContent[1]);
                    indexEntries.append("\n");
                }

            } else {


                boolean columnOnePresent = false;
                boolean columnTwoPresent = false;
                boolean columnThreePresent = false;
                boolean straysPresent = false;
                //determine columns
                for (int i = 0; i < sections.length; i++) {

                    measureAndText = sections[i].split("\t");
                    if (measureAndText.length == 2) {
                        try {
                            xAxisStart = Integer.parseInt(measureAndText[0]);
                            text = measureAndText[1];
                        } catch (NumberFormatException e) {
                            strays.append(measureAndText[1]);
                        }
                    }

                    if (xAxisStart <= columnTwoLine * .9) {
                        //use this or fixed addition for expected col w?
                        //do need factor to left too to pickup mal-aligned
                        //units (probably)
                        columnOne.append(text);
                        columnOnePresent = true;
                    } else if (xAxisStart > columnTwoLine * 0.9
                            && xAxisStart
                            < columnThreeLine * .9) {
                        columnTwo.append(text);
                        columnTwoPresent = true;
                    } else if (xAxisStart >= columnThreeLine * 0.9
                            && xAxisStart
                            < columnThreeLine * 1.5) {
                        columnThree.append(text);
                        columnThreePresent = true;
                    } else {
                        strays.append(xAxisStart + " " + text);
                    }


                }//end columnar for loop

                if (columnOnePresent) {
                    columnOne.append("\n");
                }
                if (columnTwoPresent) {
                    columnTwo.append("\n");
                }
                if (columnThreePresent) {
                    columnThree.append("\n");
                }
                if (straysPresent) {
                    strays.append("\n");
                }
            }

        }//end line iteration

        columnOne.append("[End Column One]");
        columnTwo.append("[End Column Two]");
        columnThree.append("[End Column Three]");
        strays.append("[End Non-placed Text]");
        indexEntries.append("[End Index Entries");


        List<String> columns = new ArrayList<>();
        columns.add(columnOne.toString());
        columns.add(columnTwo.toString());
        columns.add(columnThree.toString());
        columns.add(strays.toString());
        columns.add(indexEntries.toString());

        page.setColumns(columns);


        for (String column : columns) {
            System.out.println(column);
        }

        return page;

    }


    protected Page processCRDirectoryOfOfficials(Page page) {

        List<String> lines = page.getPageContents();
        int numColsOnLine = 0;
        int lineCounter = 0;
        String pNum = "";

        String[] measureAndText;
        String text = "";
        int xAxisStart = 0;

        boolean indexFlag = false;


        Pattern firstLineIssuePagination = Pattern.compile(
                "[0-9]{1,3}\\t([0-9]{1,3})\\s*");
        Matcher pageMatcher = firstLineIssuePagination.matcher("");

        Pattern colBreak = Pattern.compile("\\|");

        StringBuilder columnOne = new StringBuilder();
        StringBuilder columnTwo = new StringBuilder();
        StringBuilder columnThree = new StringBuilder();
        StringBuilder strays = new StringBuilder();
        strays.append("[Couldn't place these:] \n");
        StringBuilder indexEntries = new StringBuilder();

        for (String line : lines) {

            String[] sections = colBreak.split(line);
            numColsOnLine = sections.length;
            lineCounter++;
            //remember this means lineCounter variable is one greater than 
            //the list index through all following logic

            int columnOneLine = 74;
            int columnTwoLine = 305;
            //int columnThreeLine = 380;


            boolean columnOnePresent = false;
            boolean columnTwoPresent = false;
            boolean columnThreePresent = false;
            boolean straysPresent = false;
            //determine columns
            for (int i = 0; i < sections.length; i++) {

                measureAndText = sections[i].split("\t");
                if (measureAndText.length == 2) {
                    try {
                        xAxisStart = Integer.parseInt(measureAndText[0]);
                        text = measureAndText[1];
                    } catch (NumberFormatException e) {
                        strays.append(measureAndText[1]);
                    }
                }

                if (xAxisStart <= columnTwoLine * .9) {
                    //use this or fixed addition for expected col w?
                    //do need factor to left too to pickup mal-aligned
                    //units (probably)
                    columnOne.append(text);
                    columnOnePresent = true;
                } else if (xAxisStart > columnTwoLine) {
                    columnTwo.append(text);
                    columnTwoPresent = true;

                } else {
                    strays.append(xAxisStart + " " + text);
                }


            }//end columnar for loop

            if (columnOnePresent) {
                columnOne.append("\n");
            }
            if (columnTwoPresent) {
                columnTwo.append("\n");
            }

            if (straysPresent) {
                strays.append("\n");
            }
        }

        //end line iteration

        columnOne.append("[End Column One]");
        columnTwo.append("[End Column Two]");

        strays.append("[End Non-placed Text]");


        List<String> columns = new ArrayList<>();
        columns.add(columnOne.toString());
        columns.add(columnTwo.toString());

        columns.add(strays.toString());


        page.setColumns(columns);


        for (String column : columns) {
            System.out.println(column);
        }

        return page;


    }


    public void printIssue(IssueModel issue, String fileName) throws
            FileNotFoundException, UnsupportedEncodingException {

        try {
            List<Page> pages = issue.getPages();
            Writer fileOut;


            fileOut = (new BufferedWriter(new PrintWriter(fileName, "UTF-8")));


            for (Page page : pages) {

                List<String> columns = page.getColumns();
                String issuePageNumber = String.valueOf(page.getPageNum());
                String indexPageNumber = String.valueOf(page.getIndexPageNum());


                try {


                    fileOut.append("[Page" + issuePageNumber + "]\n");
                    fileOut.append("[Indexed (running) page" + indexPageNumber
                            + "]\n");
                    if (!(columns == null)) {
                        for (String column : columns) {

                            fileOut.append(column);


                        }
                    }
                    fileOut.write("[end page]");


                } catch (IOException ex) {
                    Logger.getLogger(IssueProcessor.class.getName())
                            .log(Level.SEVERE, null, ex);
                }

            }

            fileOut.flush();
            fileOut.close();
        } catch (IOException ex) {
            Logger.getLogger(IssueProcessor.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

    }


}
