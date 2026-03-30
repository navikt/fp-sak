package no.nav.foreldrepenger.dokumentbestiller.formidling.klient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.dokumentbestiller.formidling.Dokument;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentBestillingHtmlDto;
import no.nav.foreldrepenger.kontrakter.formidling.v3.DokumentForhåndsvisDto;

@ApplicationScoped
public class FormidlingDokument implements Dokument {

    private static final Environment ENV = Environment.current();

    private FormidlingRestKlientFss fss;
    private FormidlingRestKlientGcp gcp;

    FormidlingDokument() {
        // for CDI proxy
    }

    @Inject
    public FormidlingDokument(FormidlingRestKlientFss fss, FormidlingRestKlientGcp gcp) {
        this.fss = fss;
        this.gcp = gcp;
    }

    private FormidlingRestKlient klient() {
        return ENV.isDev() ? gcp : fss;
    }

    @Override
    public void bestill(DokumentBestillingDto dokumentBestillingDto) {
        klient().bestill(dokumentBestillingDto);
    }

    @Override
    public byte[] forhåndsvis(DokumentForhåndsvisDto dokumentForhåndsvisDto) {
        return klient().forhåndsvis(dokumentForhåndsvisDto);
    }

    @Override
    public String genererHtml(DokumentBestillingHtmlDto dokumentForhåndsvisDto) {
        return klient().genererHtml(dokumentForhåndsvisDto);
    }
}
