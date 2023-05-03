package cetus.analysis.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;

import cetus.analysis.AnalysisPass;
import cetus.hir.DFIterator;
import cetus.hir.Loop;
import cetus.hir.Program;

public class SolrIndexer extends AnalysisPass {

    private Logger logger = Logger.getLogger(this.getClass().getSimpleName());
    private SolrClient solrClient;

    protected SolrIndexer(Program program) {
        super(program);
    }

    @Override
    public String getPassName() {
        return "SolrIndexer";
    }

    @Override
    public void start() {
        solrClient = new Http2SolrClient.Builder("http://localhost:8983/solr")
                .withConnectionTimeout(60, TimeUnit.MINUTES)
                .build();
        logger.info("Starting Solr Indexer");
        List<SolrInputDocument> inDocuments = mapLoopToDocuments(getLoops(program));
        try {
            for (SolrInputDocument inDoc : inDocuments) {
                // inDoc.addField("source_file", program.);
                solrClient.add("loops", inDoc);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }

    }

    private List<Loop> getLoops(Program progam) {
        List<Loop> loops = new ArrayList<>();
        new DFIterator<Loop>(program).forEachRemaining(loops::add);
        return loops;
    }

    private List<SolrInputDocument> mapLoopToDocuments(List<Loop> loops) {
        List<SolrInputDocument> documents = StreamSupport
                .stream(loops.spliterator(), true)
                .map(this::mapLoopToDocument)
                .collect(Collectors.toList());
        return documents;
    }

    private SolrInputDocument mapLoopToDocument(Loop loop) {
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("category", "loop");
        doc.addField("code", loop);
        return doc;
    }

}
