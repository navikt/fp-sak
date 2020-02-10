package no.nav.foreldrepenger.dokumentarkiv.journal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivFilType;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.Kommunikasjonsretning;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostJournalpostIkkeInngaaende;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.binding.HentJournalpostUgyldigInput;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Aktoer;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Arkivfiltyper;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinformasjon;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentinnhold;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Dokumentkategorier;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.DokumenttypeIder;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.InngaaendeJournalpost;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Journaltilstand;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Person;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostRequest;
import no.nav.tjeneste.virksomhet.inngaaendejournal.v1.meldinger.HentJournalpostResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.inngaaendejournal.InngaaendeJournalConsumer;

@ApplicationScoped
public class InngåendeJournalAdapter {

    private static Map<Journaltilstand, JournalMetadata.Journaltilstand> journaltilstandPrjournaltilstandJaxb;

    static {
        journaltilstandPrjournaltilstandJaxb = new EnumMap<>(Journaltilstand.class);
        journaltilstandPrjournaltilstandJaxb.put(Journaltilstand.MIDLERTIDIG, JournalMetadata.Journaltilstand.MIDLERTIDIG);
        journaltilstandPrjournaltilstandJaxb.put(Journaltilstand.UTGAAR, JournalMetadata.Journaltilstand.UTGAAR);
        journaltilstandPrjournaltilstandJaxb.put(Journaltilstand.ENDELIG, JournalMetadata.Journaltilstand.ENDELIG);
    }

    private InngaaendeJournalConsumer consumer;

    InngåendeJournalAdapter() {
        // for CDI proxy
    }

    @Inject
    public InngåendeJournalAdapter(InngaaendeJournalConsumer consumer) {
        this.consumer = consumer;
    }


    public List<JournalMetadata> hentMetadata(JournalpostId journalpostId) {
        HentJournalpostResponse response = doHentJournalpost(journalpostId);
        List<JournalMetadata> metadataList = new ArrayList<>();
        konverterTilMetadata(journalpostId, response, metadataList);
        return metadataList;
    }

    public ArkivJournalPost hentInngåendeJournalpostHoveddokument(JournalpostId journalpostId) {
        InngaaendeJournalpost response = doHentJournalpost(journalpostId).getInngaaendeJournalpost();
        Dokumentinformasjon hoved = response.getHoveddokument();
        ArkivDokument.Builder dokBuilder = ArkivDokument.Builder.ny()
            .medDokumentKategori(hoved.getDokumentkategori() != null ? DokumentKategori.finnForKodeverkEiersKode(hoved.getDokumentkategori().getValue()) : DokumentKategori.UDEFINERT)
            .medDokumentTypeId(hoved.getDokumenttypeId() != null ? DokumentTypeId.finnForKodeverkEiersKode(hoved.getDokumenttypeId().getValue()) : DokumentTypeId.UDEFINERT)
            .medDokumentId(hoved.getDokumentId())
            .medTittel("");

        ArkivJournalPost arkivJournalPost = ArkivJournalPost.Builder.ny()
            .medSaksnummer(response.getArkivSak() != null ? new Saksnummer(response.getArkivSak().getArkivSakId()) : null)
            .medJournalpostId(journalpostId)
            .medTidspunkt(DateUtil.convertToLocalDateTime(response.getForsendelseMottatt()))
            .medKommunikasjonsretning(Kommunikasjonsretning.INN)
            .medKanalreferanse(response.getKanalReferanseId())
            .medBeskrivelse("")
            .medJournalFørendeEnhet(response.getJournalfEnhet())
            .medHoveddokument(dokBuilder.build())
            .build();

        return arkivJournalPost;
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

    private void konverterTilMetadata(JournalpostId journalpostId, HentJournalpostResponse response, List<JournalMetadata> metadataList) {
        InngaaendeJournalpost journalpost = response.getInngaaendeJournalpost();

        konverterTilMetadata(journalpostId, journalpost, journalpost.getHoveddokument(), true, metadataList);
        if (journalpost.getVedleggListe() != null) {
            for (Dokumentinformasjon dokumentinfo : journalpost.getVedleggListe()) {
                konverterTilMetadata(journalpostId, journalpost, dokumentinfo, false, metadataList);
            }
        }
    }

    private void konverterTilMetadata(JournalpostId journalpostId, InngaaendeJournalpost journalpost,
                                      Dokumentinformasjon dokumentinfo, boolean erHoveddokument,
                                      List<JournalMetadata> metadataList) {

        Journaltilstand journaltilstandJaxb = journalpost.getJournaltilstand();
        JournalMetadata.Journaltilstand journaltilstand = journaltilstandJaxb != null ? journaltilstandPrjournaltilstandJaxb.get(journaltilstandJaxb) : null;

        LocalDate forsendelseMottatt = DateUtil.convertToLocalDate(journalpost.getForsendelseMottatt());

        List<Aktoer> brukerListe = journalpost.getBrukerListe();

        final String dokumentId = dokumentinfo.getDokumentId();
        final DokumentTypeId dokumentTypeId = getDokumentTypeId(dokumentinfo);
        final DokumentKategori dokumentKategori = getDokumentKategori(dokumentinfo);

        List<String> brukerIdentList = brukerListe.stream().filter((a) -> {
            // instanceof OK - eksternt grensesnitt
            return a instanceof Person;  // NOSONAR
        }).map(a -> ((Person) a).getIdent()).collect(Collectors.toList());

        for (Dokumentinnhold dokumentinnhold : dokumentinfo.getDokumentInnholdListe()) {
            VariantFormat variantFormat = getVariantFormat(dokumentinnhold);
            ArkivFilType arkivFilType = getArkivFilType(dokumentinnhold);

            JournalMetadata.Builder builder = JournalMetadata.builder();
            builder.medJournalpostId(journalpostId);
            builder.medDokumentId(dokumentId);
            builder.medVariantFormat(variantFormat);
            builder.medDokumentType(dokumentTypeId);
            builder.medDokumentKategori(dokumentKategori);
            builder.medArkivFilType(arkivFilType);
            builder.medJournaltilstand(journaltilstand);
            builder.medErHoveddokument(erHoveddokument);
            builder.medForsendelseMottatt(forsendelseMottatt);
            builder.medBrukerIdentListe(brukerIdentList);
            JournalMetadata metadata = builder.build();

            metadataList.add(metadata);
        }
    }

    private DokumentTypeId getDokumentTypeId(Dokumentinformasjon dokumentinfo) {
        DokumentTypeId dokumentTypeId = null;
        DokumenttypeIder dokumenttypeJaxb = dokumentinfo.getDokumenttypeId();
        if (dokumenttypeJaxb != null && dokumenttypeJaxb.getValue() != null) {
            final String offisiellKode = dokumenttypeJaxb.getValue();
            dokumentTypeId = DokumentTypeId.finnForKodeverkEiersKode(offisiellKode);
        }
        return dokumentTypeId;
    }

    private DokumentKategori getDokumentKategori(Dokumentinformasjon dokumentinfo) {
        DokumentKategori dokumentKategori = null;
        Dokumentkategorier dokumentkategoriJaxb = dokumentinfo.getDokumentkategori();
        if (dokumentkategoriJaxb != null && dokumentkategoriJaxb.getValue() != null) {
            String offisiellKode = dokumentkategoriJaxb.getValue();
            dokumentKategori = DokumentKategori.finnForKodeverkEiersKode(offisiellKode);
        }
        return dokumentKategori;
    }

    private VariantFormat getVariantFormat(Dokumentinnhold dokumentinnhold) {
        VariantFormat variantFormat = null;
        Variantformater variantformatJaxb = dokumentinnhold.getVariantformat();
        if (variantformatJaxb != null && variantformatJaxb.getValue() != null) {
            String offisiellKode = variantformatJaxb.getValue();
            variantFormat = VariantFormat.finnForKodeverkEiersKode(offisiellKode);
        }
        return variantFormat;
    }

    private ArkivFilType getArkivFilType(Dokumentinnhold dokumentinnhold) {
        ArkivFilType arkivFilType = null;
        Arkivfiltyper arkivfiltypeJaxb = dokumentinnhold.getArkivfiltype();
        if (arkivfiltypeJaxb != null && arkivfiltypeJaxb.getValue() != null) {
            String offisiellKode = arkivfiltypeJaxb.getValue();
            arkivFilType = ArkivFilType.finnForKodeverkEiersKode(offisiellKode);
        }
        return arkivFilType;
    }
}
