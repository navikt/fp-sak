package no.nav.foreldrepenger.web.app.tjenester.behandling.historikk;

import java.net.URI;

public class HistorikkInnslagDokumentLinkDto {

    private String tag;
    private URI url;

    private String journalpostId;
    private String dokumentId;
    private boolean utgått;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getDokumentId() {
        return dokumentId;
    }

    public void setDokumentId(String dokumentId) {
        this.dokumentId = dokumentId;
    }

    public URI getUrl() {
        return url;
    }

    public void setUrl(URI url) {
        this.url = url;
    }

    public void setUrl(String url) {
        this.url = URI.create(url);
    }

    public boolean isUtgått() {
        return utgått;
    }

    public void setUtgått(boolean utgått) {
        this.utgått = utgått;
    }
}
