package no.nav.foreldrepenger.dokumentarkiv.journal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.Kommunikasjonsretning;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinformasjon;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.InngaaendeJournalpost;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.inngaaendejournal.InngaaendeJournalConsumer;

@ApplicationScoped
public class InngåendeJournalAdapter {

    private InngaaendeJournalConsumer consumer;

    InngåendeJournalAdapter() {
        // for CDI proxy
    }

    @Inject
    public InngåendeJournalAdapter(InngaaendeJournalConsumer consumer) {
        this.consumer = consumer;
    }


    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        InngaaendeJournalpost response = doHentJournalpost(journalpostId).getInngaaendeJournalpost();
        Dokumentinformasjon hoved = response.getHoveddokument();
        ArkivDokument.Builder dokBuilder = ArkivDokument.Builder.ny()
            .medDokumentKategori(hoved.getDokumentkategori() != null ? DokumentKategori.finnForKodeverkEiersKode(hoved.getDokumentkategori().getValue()) : DokumentKategori.UDEFINERT)
            .medDokumentTypeId(hoved.getDokumenttypeId() != null ? DokumentTypeId.finnForKodeverkEiersKode(hoved.getDokumenttypeId().getValue()) : DokumentTypeId.UDEFINERT)
            .medDokumentId(hoved.getDokumentId())
            .medTittel("");

        return ArkivJournalPost.Builder.ny()
            .medSaksnummer(response.getArkivSak() != null ? new Saksnummer(response.getArkivSak().getArkivSakId()) : null)
            .medJournalpostId(journalpostId)
            .medTidspunkt(DateUtil.convertToLocalDateTime(response.getForsendelseMottatt()))
            .medKommunikasjonsretning(Kommunikasjonsretning.INN)
            .medKanalreferanse(response.getKanalReferanseId())
            .medBeskrivelse("")
            .medJournalFørendeEnhet(response.getJournalfEnhet())
            .medHoveddokument(dokBuilder.build())
            .build();
    }

    private HentJournalpostResponse doHentJournalpost(JournalpostId journalpostId) {

        HentJournalpostRequest request = new HentJournalpostRequest();
        request.setJournalpostId(journalpostId.getVerdi());

        HentJournalpostResponse response;
        try {
            response = consumer.hentJournalpost(request);
        } catch (HentJournalpostJournalpostIkkeFunnet e) {
            throw JournalFeil.FACTORY.hentJournalpostIkkeFunnet(e).toException();
        } catch (HentJournalpostSikkerhetsbegrensning e) {
            throw JournalFeil.FACTORY.journalUtilgjengeligSikkerhetsbegrensning("Hent metadata", e).toException();
        } catch (HentJournalpostUgyldigInput e) {
            throw JournalFeil.FACTORY.journalpostUgyldigInput(e).toException();
        } catch (HentJournalpostJournalpostIkkeInngaaende e) {
            throw JournalFeil.FACTORY.journalpostIkkeInngaaende(e).toException();
        }

        return response;
    }

}
