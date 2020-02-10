package no.nav.foreldrepenger.dokumentarkiv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.arkiv.DokumentTypeIdKodeliste;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journaltilstand;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.ArkivSak;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumer;

@ApplicationScoped
public class DokumentArkivTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);
    private JournalConsumer journalConsumer;

    private FagsakRepository fagsakRepository;

    private KodeverkRepository kodeverkRepository;

    private final Set<ArkivFilType> filTyperPdf = byggArkivFilTypeSet();
    private final VariantFormat variantFormatArkiv = VariantFormat.ARKIV;


    DokumentArkivTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DokumentArkivTjeneste(JournalConsumer journalConsumer, FagsakRepository fagsakRepository, KodeverkRepository kodeverkRepository) {
        this.journalConsumer = journalConsumer;
        this.fagsakRepository = fagsakRepository;
        this.kodeverkRepository = kodeverkRepository;
    }

    public byte[] hentDokumnet(JournalpostId journalpostId, String dokumentId) {
        LOG.info("HentDokument: input parametere journalpostId {} dokumentId {}", journalpostId, dokumentId);
        byte[] pdfFile = new byte[0];
        HentDokumentRequest hentDokumentRequest = new HentDokumentRequest();
        hentDokumentRequest.setJournalpostId(journalpostId.getVerdi());
        hentDokumentRequest.setDokumentId(dokumentId);
        Variantformater variantFormat = new Variantformater();
        variantFormat.setValue(variantFormatArkiv.getOffisiellKode());
        hentDokumentRequest.setVariantformat(variantFormat);

        try {
            HentDokumentResponse hentDokumentResponse = journalConsumer.hentDokument(hentDokumentRequest);
            if (hentDokumentResponse != null && hentDokumentResponse.getDokument() != null) {
                pdfFile = hentDokumentResponse.getDokument();
            }
        } catch (HentDokumentDokumentIkkeFunnet e) {
            throw DokumentArkivTjenesteFeil.FACTORY.hentDokumentIkkeFunnet(e).toException();
        } catch (HentDokumentJournalpostIkkeFunnet e) {
            throw DokumentArkivTjenesteFeil.FACTORY.hentJournalpostIkkeFunnet(e).toException();
        } catch (HentDokumentSikkerhetsbegrensning e) {
            throw DokumentArkivTjenesteFeil.FACTORY.journalUtilgjengeligSikkerhetsbegrensning("hent dokument", e).toException();
        }
        return pdfFile;
    }

    public List<ArkivJournalPost> hentAlleDokumenterForVisning(Saksnummer saksnummer) {
        List<ArkivJournalPost> journalPosterForSak = hentAlleJournalposterForSak(saksnummer);

        List<ArkivJournalPost> journalPosts = new ArrayList<>();

        journalPosterForSak.forEach(jpost -> {
            if (!erDokumentArkivPdf(jpost.getHovedDokument())) {
                jpost.setHovedDokument(null);
            }

            jpost.getAndreDokument().forEach(dok -> {
                if (!erDokumentArkivPdf(jpost.getHovedDokument())) {
                    jpost.getAndreDokument().remove(dok);
                }
            });
        });
        journalPosterForSak.stream()
            .filter(jpost -> jpost.getHovedDokument() != null || !jpost.getAndreDokument().isEmpty())
            .forEach(journalPosts::add);

        return journalPosts;
    }

    private boolean erDokumentArkivPdf(ArkivDokument arkivDokument) {
        for (ArkivDokumentHentbart format : arkivDokument.getTilgjengeligSom()) {
            if (variantFormatArkiv.equals(format.getVariantFormat()) && filTyperPdf.contains(format.getArkivFilType())) {
                return true;
            }
        }
        return false;
    }

    public List<ArkivJournalPost> hentAlleJournalposterForSak(Saksnummer saksnummer) {
        List<ArkivJournalPost> journalPosts = new ArrayList<>();
        doHentKjerneJournalpostListe(saksnummer)
            .map(HentKjerneJournalpostListeResponse::getJournalpostListe).orElse(new ArrayList<>())
            .stream()
            .filter(journalpost -> !Journaltilstand.UTGAAR.equals(journalpost.getJournaltilstand()))
            .forEach(journalpost -> {
                ArkivJournalPost.Builder arkivJournalPost = opprettArkivJournalPost(saksnummer, journalpost);
                journalPosts.add(arkivJournalPost.build());
            });

        return journalPosts;
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(Saksnummer saksnummer, JournalpostId journalpostId) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer);
        return journalPosts.stream().filter(jpost -> journalpostId.equals(jpost.getJournalpostId())).findFirst();
    }

    public Set<DokumentType> hentDokumentTypeIdForSak(Saksnummer saksnummer, LocalDate mottattEtterDato, List<DokumentType> eksisterende) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer);
        Set<DokumentType> alleDTID = new HashSet<>();
        journalPosts.forEach(jpost -> {
            ekstraherDTID(alleDTID, jpost.getHovedDokument());
            jpost.getAndreDokument().forEach(dok -> ekstraherDTID(alleDTID, dok));
        });
        if (LocalDate.MIN.equals(mottattEtterDato)) {
            return alleDTID;
        }
        Set<DokumentType> etterDato = new HashSet<>();
        journalPosts.stream().filter(jpost -> jpost.getTidspunkt() != null && !jpost.getTidspunkt().toLocalDate().isBefore(mottattEtterDato))
            .forEach(jpost -> {
                ekstraherDTID(etterDato, jpost.getHovedDokument());
                jpost.getAndreDokument().forEach(dok -> ekstraherDTID(etterDato, dok));
            });
        alleDTID.stream().filter(dtid -> !etterDato.contains(dtid))
            .forEach(dtid -> {
                if (eksisterende.contains(dtid)) {
                    etterDato.add(dtid);
                }
            });
        return etterDato;
    }

    public DokumentType utledDokumentTypeFraTittel(Saksnummer saksnummer, JournalpostId journalpostId) {
        ArkivJournalPost arkivJournalPost = hentJournalpostForSak(saksnummer, journalpostId).orElse(null);
        if (arkivJournalPost == null || arkivJournalPost.getHovedDokument() == null || arkivJournalPost.getHovedDokument().getTittel() == null) {
            return DokumentTypeId.UDEFINERT;
        }

        var opt = kodeverkRepository.finnForKodeverkEiersNavn(DokumentTypeIdKodeliste.class, arkivJournalPost.getHovedDokument().getTittel());
        if(opt.isPresent()) {
            return opt.get();
        } else {
            return DokumentTypeId.UDEFINERT;
        }
    }

    private void ekstraherDTID(Set<DokumentType> eksisterende, ArkivDokument dokument) {
        if (dokument == null) {
            return;
        }
        if (!eksisterende.contains(dokument.getDokumentType())) {
            eksisterende.add(dokument.getDokumentType());
        }
        for (ArkivDokumentVedlegg vedlegg : dokument.getInterneVedlegg()) {
            if (!eksisterende.contains(vedlegg.getDokumentTypeId())) {
                eksisterende.add(vedlegg.getDokumentTypeId());
            }
        }
    }

    private Optional<HentKjerneJournalpostListeResponse> doHentKjerneJournalpostListe(Saksnummer saksnummer) {
        final Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (!fagsak.isPresent()) {
            return Optional.empty();
        }
        HentKjerneJournalpostListeRequest hentKjerneJournalpostListeRequest = new HentKjerneJournalpostListeRequest();

        hentKjerneJournalpostListeRequest.getArkivSakListe().add(lageJournalSak(saksnummer, Fagsystem.GOSYS.getOffisiellKode()));

        try {
            HentKjerneJournalpostListeResponse hentKjerneJournalpostListeResponse = journalConsumer
                .hentKjerneJournalpostListe(hentKjerneJournalpostListeRequest);
            return Optional.of(hentKjerneJournalpostListeResponse);
        } catch (HentKjerneJournalpostListeSikkerhetsbegrensning e) {
            throw DokumentArkivTjenesteFeil.FACTORY.journalUtilgjengeligSikkerhetsbegrensning("hent journalpostliste", e).toException();
        } catch (HentKjerneJournalpostListeUgyldigInput e) {
            throw DokumentArkivTjenesteFeil.FACTORY.journalpostUgyldigInput(e).toException();
        }
    }

    private static Set<ArkivFilType> byggArkivFilTypeSet() {
        final ArkivFilType arkivFilTypePdf = ArkivFilType.PDF;
        final ArkivFilType arkivFilTypePdfa = ArkivFilType.PDFA;
        return new HashSet<>(Arrays.asList(arkivFilTypePdf, arkivFilTypePdfa));
    }

    private ArkivSak lageJournalSak(Saksnummer saksnummer, String fagsystem) {
        ArkivSak journalSak = new ArkivSak();
        journalSak.setArkivSakSystem(fagsystem);
        journalSak.setArkivSakId(saksnummer.getVerdi());
        journalSak.setErFeilregistrert(false);
        return journalSak;
    }

    private ArkivJournalPost.Builder opprettArkivJournalPost(Saksnummer saksnummer, Journalpost journalpost) {
        LocalDateTime tidspunkt = journalpost.getForsendelseJournalfoert() != null ? DateUtil.convertToLocalDateTime(journalpost.getForsendelseJournalfoert())
            : DateUtil.convertToLocalDateTime(journalpost.getForsendelseMottatt());

        ArkivJournalPost.Builder builder = ArkivJournalPost.Builder.ny()
            .medSaksnummer(saksnummer)
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getInnhold())
            .medTidspunkt(tidspunkt)
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype().getValue()))
            .medHoveddokument(opprettArkivDokument(journalpost.getHoveddokument()).build());
        journalpost.getVedleggListe().forEach(vedlegg -> {
            builder.leggTillVedlegg(opprettArkivDokument(vedlegg).build());
        });
        return builder;
    }

    private ArkivDokument.Builder opprettArkivDokument(DetaljertDokumentinformasjon detaljertDokumentinformasjon) {
        ArkivDokument.Builder builder = ArkivDokument.Builder.ny()
            .medDokumentId(detaljertDokumentinformasjon.getDokumentId())
            .medTittel(detaljertDokumentinformasjon.getTittel())
            .medDokumentTypeId(detaljertDokumentinformasjon.getDokumentTypeId() != null
                ? DokumentTypeId.finnForKodeverkEiersKode(detaljertDokumentinformasjon.getDokumentTypeId().getValue())
                : DokumentTypeId.UDEFINERT)
            .medDokumentKategori(detaljertDokumentinformasjon.getDokumentkategori() != null
                ? DokumentKategori.finnForKodeverkEiersKode(detaljertDokumentinformasjon.getDokumentkategori().getValue())
                : DokumentKategori.UDEFINERT);
        detaljertDokumentinformasjon.getSkannetInnholdListe().forEach(vedlegg -> {
            builder.leggTilInterntVedlegg(ArkivDokumentVedlegg.Builder.ny()
                .medTittel(vedlegg.getVedleggInnhold())
                .medDokumentTypeId(vedlegg.getDokumenttypeId() != null ? DokumentTypeId.finnForKodeverkEiersKode(vedlegg.getDokumenttypeId().getValue())
                    : DokumentTypeId.UDEFINERT)
                .build());
        });
        detaljertDokumentinformasjon.getDokumentInnholdListe().forEach(innhold -> {
            builder.leggTilTilgjengeligFormat(ArkivDokumentHentbart.Builder.ny()
                .medArkivFilType(
                    innhold.getArkivfiltype() != null ? ArkivFilType.finnForKodeverkEiersKode(innhold.getArkivfiltype().getValue()) : ArkivFilType.UDEFINERT)
                .medVariantFormat(innhold.getVariantformat() != null ? VariantFormat.finnForKodeverkEiersKode(innhold.getVariantformat().getValue())
                    : VariantFormat.UDEFINERT)
                .build());
        });
        return builder;
    }
}
