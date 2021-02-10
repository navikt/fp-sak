package no.nav.foreldrepenger.dokumentarkiv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.saf.DokumentInfo;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentoversiktFagsakQueryRequest;
import no.nav.saf.DokumentoversiktResponseProjection;
import no.nav.saf.DokumentvariantResponseProjection;
import no.nav.saf.FagsakInput;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.Journalstatus;
import no.nav.saf.LogiskVedleggResponseProjection;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.HentKjerneJournalpostListeUgyldigInput;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.DokumenttypeIder;
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
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;
import no.nav.vedtak.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.vedtak.felles.integrasjon.saf.Saf;

@ApplicationScoped
public class DokumentArkivTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);
    private static final Long SAKSNUMMER_TRANSISJON = 152000000L;

    private JournalConsumer journalConsumer;
    private Saf safKlient;
    private FagsakRepository fagsakRepository;

    private final Set<ArkivFilType> filTyperPdf = byggArkivFilTypeSet();
    private static final VariantFormat VARIANT_FORMAT_ARKIV = VariantFormat.ARKIV;


    DokumentArkivTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DokumentArkivTjeneste(JournalConsumer journalConsumer, @Jersey Saf safTjeneste, FagsakRepository fagsakRepository) {
        this.journalConsumer = journalConsumer;
        this.safKlient = safTjeneste;
        this.fagsakRepository = fagsakRepository;
    }

    public byte[] hentDokument(Saksnummer saksnummer, JournalpostId journalpostId, String dokumentId) {
        LOG.info("HentDokument: input parametere journalpostId {} dokumentId {}", journalpostId, dokumentId);
        if (Long.parseLong(saksnummer.getVerdi()) > SAKSNUMMER_TRANSISJON) {
            var query = new HentDokumentQuery(journalpostId.getVerdi(), dokumentId, VARIANT_FORMAT_ARKIV.getOffisiellKode());
            return safKlient.hentDokument(query);
        }
        byte[] pdfFile = new byte[0];
        HentDokumentRequest hentDokumentRequest = new HentDokumentRequest();
        hentDokumentRequest.setJournalpostId(journalpostId.getVerdi());
        hentDokumentRequest.setDokumentId(dokumentId);
        Variantformater variantFormat = new Variantformater();
        variantFormat.setValue(VARIANT_FORMAT_ARKIV.getOffisiellKode());
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
            if (jpost.getHovedDokument() != null && !erDokumentArkivPdf(jpost.getHovedDokument())) {
                jpost.setHovedDokument(null);
            }

            jpost.getAndreDokument().forEach(dok -> {
                if (!erDokumentArkivPdf(dok)) {
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
        return arkivDokument.getTilgjengeligSom().stream()
            .filter(f -> f.getVariantFormat() != null)
            .anyMatch(f -> VARIANT_FORMAT_ARKIV.equals(f.getVariantFormat()) && (f.getArkivFilType() == null || filTyperPdf.contains(f.getArkivFilType())));
    }

    public List<ArkivJournalPost> hentAlleJournalposterForSak(Saksnummer saksnummer) {
        if (Long.parseLong(saksnummer.getVerdi()) > SAKSNUMMER_TRANSISJON) {
            return doHentJournalpostListe(saksnummer, Set.of(Journalstatus.UTGAAR));
        }
        List<ArkivJournalPost> journalPosts = new ArrayList<>();
        doHentKjerneJournalpostListe(saksnummer)
            .map(HentKjerneJournalpostListeResponse::getJournalpostListe).orElse(new ArrayList<>())
            .stream()
            .filter(journalpost -> !Journaltilstand.UTGAAR.equals(journalpost.getJournaltilstand()))
            .forEach(journalpost -> {
                ArkivJournalPost.Builder arkivJournalPost = opprettArkivJournalPost(journalpost);
                journalPosts.add(arkivJournalPost.build());
            });

        return journalPosts;
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(Saksnummer saksnummer, JournalpostId journalpostId) {
        if (Long.parseLong(saksnummer.getVerdi()) > SAKSNUMMER_TRANSISJON) {
            return doHentJournalpost(journalpostId);
        }
        return doHentKjerneJournalpostListe(saksnummer)
            .map(HentKjerneJournalpostListeResponse::getJournalpostListe).orElse(new ArrayList<>())
            .stream()
            .filter(journalpost -> journalpostId.getVerdi().equals(journalpost.getJournalpostId()))
            .findFirst()
            .map(journalpost -> opprettArkivJournalPost(journalpost).build());
    }

    public Set<DokumentTypeId> hentDokumentTypeIdForSak(Saksnummer saksnummer, LocalDate mottattEtterDato) {
        List<ArkivJournalPost> journalPosts = hentAlleJournalposterForSak(saksnummer).stream()
            .filter(ajp -> Kommunikasjonsretning.INN.equals(ajp.getKommunikasjonsretning()))
            .collect(Collectors.toList());
        Set<DokumentTypeId> alleDTID = new HashSet<>();
        if (LocalDate.MIN.equals(mottattEtterDato)) {
            journalPosts.forEach(jpost -> ekstraherJournalpostDTID(alleDTID, jpost));
        } else {
            journalPosts.stream()
                .filter(jpost -> jpost.getTidspunkt() != null && jpost.getTidspunkt().isAfter(mottattEtterDato.atStartOfDay()))
                .forEach(jpost -> ekstraherJournalpostDTID(alleDTID, jpost));
        }
        return alleDTID;
    }

    private void ekstraherJournalpostDTID(Set<DokumentTypeId> alleDTID, ArkivJournalPost jpost) {
        dokumentTypeFraTittel(jpost.getBeskrivelse()).ifPresent(alleDTID::add);
        ekstraherDokumentDTID(alleDTID, jpost.getHovedDokument());
        jpost.getAndreDokument().forEach(dok -> ekstraherDokumentDTID(alleDTID, dok));
    }

    private void ekstraherDokumentDTID(Set<DokumentTypeId> eksisterende, ArkivDokument dokument) {
        Optional.ofNullable(dokument).map(ArkivDokument::getAlleDokumenttyper).ifPresent(eksisterende::addAll);
    }

    private Optional<HentKjerneJournalpostListeResponse> doHentKjerneJournalpostListe(Saksnummer saksnummer) {
        final Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer);
        if (fagsak.isEmpty()) {
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

    private List<ArkivJournalPost> doHentJournalpostListe(Saksnummer saksnummer, Set<Journalstatus> exclude) {
        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.getVerdi(), Fagsystem.FPSAK.getOffisiellKode()));
        query.setTema(List.of());
        query.setJournalposttyper(List.of());
        query.setJournalstatuser(List.of());

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(standardJournalpostProjection());

        var resultat = safKlient.dokumentoversiktFagsak(query, projection);

        return resultat.getJournalposter().stream()
            .filter(j -> j.getJournalstatus() == null || !exclude.contains(j.getJournalstatus()))
            .map(this::mapTilArkivJournalPost)
            .collect(Collectors.toList());

    }

    private Optional<ArkivJournalPost> doHentJournalpost(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var projection = standardJournalpostProjection();

        var resultat = safKlient.hentJournalpostInfo(query, projection);

        return Optional.ofNullable(resultat).map(this::mapTilArkivJournalPost);
    }

    private JournalpostResponseProjection standardJournalpostProjection() {
        return new JournalpostResponseProjection()
            .journalpostId()
            .journalposttype()
            .tittel()
            .journalstatus()
            .datoOpprettet()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat())
                .logiskeVedlegg(new LogiskVedleggResponseProjection().tittel()));
    }

    private ArkivJournalPost mapTilArkivJournalPost(no.nav.saf.Journalpost journalpost) {

        var dokumenter = journalpost.getDokumenter().stream()
            .map(this::mapTilArkivDokument)
            .collect(Collectors.toList());
        var hoveddokumentType = utledHovedDokumentType(dokumenter.stream().map(ArkivDokument::getDokumentType).collect(Collectors.toSet()));
        var hoveddokument = dokumenter.stream().filter(d -> hoveddokumentType.equals(d.getDokumentType())).findFirst();

        ArkivJournalPost.Builder builder = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getTittel())
            .medTidspunkt(journalpost.getDatoOpprettet() == null ? null : LocalDateTime.ofInstant(journalpost.getDatoOpprettet().toInstant(), ZoneId.systemDefault()))
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype().name()));
        hoveddokument.ifPresent(builder::medHoveddokument);
        dokumenter.stream()
            .filter(d -> hoveddokument.map(hd -> !hd.getDokumentId().equals(d.getDokumentId())).orElse(true))
            .forEach(builder::leggTillVedlegg);
        return builder.build();
    }

    private ArkivDokument mapTilArkivDokument(DokumentInfo dokumentInfo) {
        var alleDokumenttyper = utledDokumentType(dokumentInfo);
        ArkivDokument.Builder builder = ArkivDokument.Builder.ny()
            .medDokumentId(dokumentInfo.getDokumentInfoId())
            .medTittel(dokumentInfo.getTittel())
            .medAlleDokumenttyper(alleDokumenttyper)
            .medDokumentTypeId(utledHovedDokumentType(alleDokumenttyper)); // utvid med brevkode

        dokumentInfo.getDokumentvarianter().forEach(innhold -> {
            builder.leggTilTilgjengeligFormat(ArkivDokumentHentbart.Builder.ny()
                .medVariantFormat(innhold.getVariantformat() != null ? VariantFormat.finnForKodeverkEiersKode(innhold.getVariantformat().name())
                    : VariantFormat.UDEFINERT)
                .build());
        });
        return builder.build();
    }

    private Set<DokumentTypeId> utledDokumentType(DokumentInfo dokumentInfo) {
        Set<NAVSkjema> allebrevkoder = new HashSet<>();
        allebrevkoder.add(NAVSkjema.fraTermNavn(dokumentInfo.getTittel()));
        dokumentInfo.getLogiskeVedlegg().forEach(v -> allebrevkoder.add(NAVSkjema.fraTermNavn(v.getTittel())));
        Optional.ofNullable(dokumentInfo.getBrevkode()).map(NAVSkjema::fraOffisiellKode).ifPresent(allebrevkoder::add);

        Set<DokumentTypeId> alletyper = new HashSet<>();
        alletyper.add(DokumentTypeId.finnForKodeverkEiersNavn(dokumentInfo.getTittel()));
        dokumentInfo.getLogiskeVedlegg().forEach(v -> alletyper.add(DokumentTypeId.finnForKodeverkEiersNavn(v.getTittel())));
        allebrevkoder.stream().filter(b -> !NAVSkjema.UDEFINERT.equals(b)).forEach(b -> alletyper.add(MapNAVSkjemaDokumentTypeId.mapBrevkode(b)));
        return alletyper;
    }

    private static DokumentTypeId utledHovedDokumentType(Set<DokumentTypeId> alleTyper) {
        int lavestrank = alleTyper.stream()
            .map(MapNAVSkjemaDokumentTypeId::dokumentTypeRank)
            .min(Comparator.naturalOrder()).orElse(MapNAVSkjemaDokumentTypeId.UDEF_RANK);
        if (lavestrank == MapNAVSkjemaDokumentTypeId.GEN_RANK) {
            return alleTyper.stream()
                .filter(t -> MapNAVSkjemaDokumentTypeId.dokumentTypeRank(t) == MapNAVSkjemaDokumentTypeId.GEN_RANK)
                .findFirst().orElse(DokumentTypeId.UDEFINERT);
        }
        return MapNAVSkjemaDokumentTypeId.dokumentTypeFromRank(lavestrank);
    }

    private ArkivJournalPost.Builder opprettArkivJournalPost(Journalpost journalpost) {
        LocalDateTime tidspunkt = journalpost.getForsendelseJournalfoert() != null ? DateUtil.convertToLocalDateTime(journalpost.getForsendelseJournalfoert())
            : DateUtil.convertToLocalDateTime(journalpost.getForsendelseMottatt());

        ArkivJournalPost.Builder builder = ArkivJournalPost.Builder.ny()
            .medJournalpostId(new JournalpostId(journalpost.getJournalpostId()))
            .medBeskrivelse(journalpost.getInnhold())
            .medTidspunkt(tidspunkt)
            .medKommunikasjonsretning(Kommunikasjonsretning.fromKommunikasjonsretningCode(journalpost.getJournalposttype().getValue()))
            .medHoveddokument(opprettArkivDokument(journalpost.getHoveddokument()));
        journalpost.getVedleggListe().forEach(vedlegg -> builder.leggTillVedlegg(opprettArkivDokument(vedlegg)));
        return builder;
    }

    private ArkivDokument opprettArkivDokument(DetaljertDokumentinformasjon detaljertDokumentinformasjon) {
        Set<DokumentTypeId> alleTyper = new HashSet<>();
        alleTyper.add(utledDokumentType(detaljertDokumentinformasjon.getDokumentTypeId()));
        dokumentTypeFraTittel(detaljertDokumentinformasjon.getTittel()).ifPresent(alleTyper::add);
        detaljertDokumentinformasjon.getSkannetInnholdListe()
            .forEach(vedlegg -> alleTyper.add(utledDokumentType(vedlegg.getDokumenttypeId())));
        detaljertDokumentinformasjon.getSkannetInnholdListe()
            .forEach(vedlegg -> dokumentTypeFraTittel(vedlegg.getVedleggInnhold()).ifPresent(alleTyper::add));

        ArkivDokument.Builder builder = ArkivDokument.Builder.ny()
            .medDokumentId(detaljertDokumentinformasjon.getDokumentId())
            .medTittel(detaljertDokumentinformasjon.getTittel())
            .medDokumentTypeId(utledHovedDokumentType(alleTyper))
            .medAlleDokumenttyper(alleTyper);

        detaljertDokumentinformasjon.getDokumentInnholdListe().forEach(innhold -> {
            builder.leggTilTilgjengeligFormat(ArkivDokumentHentbart.Builder.ny()
                .medArkivFilType(
                    innhold.getArkivfiltype() != null ? ArkivFilType.finnForKodeverkEiersKode(innhold.getArkivfiltype().getValue()) : ArkivFilType.UDEFINERT)
                .medVariantFormat(innhold.getVariantformat() != null ? VariantFormat.finnForKodeverkEiersKode(innhold.getVariantformat().getValue())
                    : VariantFormat.UDEFINERT)
                .build());
        });
        return builder.build();
    }

    private DokumentTypeId utledDokumentType(DokumenttypeIder dokumenttypeIder) {
        return Optional.ofNullable(dokumenttypeIder)
            .map(DokumenttypeIder::getValue)
            .map(DokumentTypeId::finnForKodeverkEiersKode).orElse(DokumentTypeId.UDEFINERT);
    }

    private Optional<DokumentTypeId> dokumentTypeFraTittel(String tittel) {
        return Optional.ofNullable(tittel).map(DokumentTypeId::finnForKodeverkEiersNavn);
    }
}
