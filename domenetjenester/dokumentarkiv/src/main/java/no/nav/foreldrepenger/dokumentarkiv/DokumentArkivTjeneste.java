package no.nav.foreldrepenger.dokumentarkiv;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.VariantFormat;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.saf.DokumentInfo;
import no.nav.saf.DokumentInfoResponseProjection;
import no.nav.saf.DokumentoversiktFagsakQueryRequest;
import no.nav.saf.DokumentoversiktResponseProjection;
import no.nav.saf.Dokumentvariant;
import no.nav.saf.DokumentvariantResponseProjection;
import no.nav.saf.FagsakInput;
import no.nav.saf.Journalpost;
import no.nav.saf.JournalpostQueryRequest;
import no.nav.saf.JournalpostResponseProjection;
import no.nav.saf.Journalposttype;
import no.nav.saf.Journalstatus;
import no.nav.saf.LogiskVedleggResponseProjection;
import no.nav.saf.TilleggsopplysningResponseProjection;
import no.nav.saf.Variantformat;
import no.nav.vedtak.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.vedtak.felles.integrasjon.saf.Saf;
import no.nav.vedtak.util.LRUCache;

@ApplicationScoped
public class DokumentArkivTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(DokumentArkivTjeneste.class);
    static final String FP_DOK_TYPE = "fp_innholdtype";

    private Saf safKlient;

    private static final VariantFormat VARIANT_FORMAT_ARKIV = VariantFormat.ARKIV;
    private static final Set<Journalstatus> EKSKLUDER_STATUS = Set.of(Journalstatus.UTGAAR);

    private static final long CACHE_ELEMENT_LIVE_TIME_MS = TimeUnit.MILLISECONDS.convert(15, TimeUnit.MINUTES);
    private LRUCache<String, List<ArkivJournalPost>> sakJournalCache = new LRUCache<>(500, CACHE_ELEMENT_LIVE_TIME_MS);


    DokumentArkivTjeneste() {
        // for CDI proxy
    }

    @Inject
    public DokumentArkivTjeneste(Saf safTjeneste) {
        this.safKlient = safTjeneste;
    }

    public byte[] hentDokument(@SuppressWarnings("unused") Saksnummer saksnummer, JournalpostId journalpostId, String dokumentId) {
        var query = new HentDokumentQuery(journalpostId.getVerdi(), dokumentId, VARIANT_FORMAT_ARKIV.getOffisiellKode());
        return safKlient.hentDokument(query);
    }

    public String hentStrukturertDokument(JournalpostId journalpostId, String dokumentId) {
        var query = new HentDokumentQuery(journalpostId.getVerdi(), dokumentId, VariantFormat.ORIGINAL.getOffisiellKode());
        return new String(safKlient.hentDokument(query));
    }

    public List<ArkivJournalPost> hentAlleDokumenterForVisning(Saksnummer saksnummer) {
        return hentAlleJournalposterForSak(saksnummer).stream()
            .map(this::kopiMedKunArkivdokument)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private ArkivJournalPost kopiMedKunArkivdokument(ArkivJournalPost journalPost) {
        var hoved = Optional.ofNullable(journalPost.getHovedDokument()).filter(this::erDokumentArkiv);
        var andre = journalPost.getAndreDokument().stream()
            .filter(this::erDokumentArkiv)
            .collect(Collectors.toList());
        if (hoved.isEmpty() && andre.isEmpty()) return null;

        return ArkivJournalPost.Builder.ny()
            .medJournalpostId(journalPost.getJournalpostId())
            .medBeskrivelse(journalPost.getBeskrivelse())
            .medKommunikasjonsretning(journalPost.getKommunikasjonsretning())
            .medHoveddokument(hoved.orElse(null))
            .medAndreDokument(andre)
            .medTidspunkt(journalPost.getTidspunkt())
            .build();
    }

    private boolean erDokumentArkiv(ArkivDokument arkivDokument) {
        return arkivDokument.getTilgjengeligSom().contains(VARIANT_FORMAT_ARKIV);
    }

    private List<ArkivJournalPost> hentAlleJournalposterForSakSjekkCache(Saksnummer saksnummer) {
        if (sakJournalCache.get(saksnummer.getVerdi()) != null && !sakJournalCache.get(saksnummer.getVerdi()).isEmpty())
            return sakJournalCache.get(saksnummer.getVerdi());
        return hentAlleJournalposterForSak(saksnummer);
    }

    public List<ArkivJournalPost> hentAlleJournalposterForSak(Saksnummer saksnummer) {
        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.getVerdi(), Fagsystem.FPSAK.getOffisiellKode()));
        query.setFoerste(1000);

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(standardJournalpostProjection());

        var resultat = safKlient.dokumentoversiktFagsak(query, projection);

        var journalposter = resultat.getJournalposter().stream()
            .filter(j -> j.getJournalstatus() == null || !EKSKLUDER_STATUS.contains(j.getJournalstatus()))
            .map(this::mapTilArkivJournalPost)
            .collect(Collectors.toList());

        sakJournalCache.put(saksnummer.getVerdi(), journalposter);
        return journalposter;
    }

    public Optional<ArkivJournalPost> hentJournalpostForSak(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var projection = standardJournalpostProjection();

        var resultat = safKlient.hentJournalpostInfo(query, projection);

        return Optional.ofNullable(resultat).map(this::mapTilArkivJournalPost);
    }

    public List<ArkivDokumentUtgående> hentAlleUtgåendeJournalposterForSak(Saksnummer saksnummer) {
        var query = new DokumentoversiktFagsakQueryRequest();
        query.setFagsak(new FagsakInput(saksnummer.getVerdi(), Fagsystem.FPSAK.getOffisiellKode()));
        query.setFoerste(1000);

        var projection = new DokumentoversiktResponseProjection()
            .journalposter(utgåendeProjection());

        var resultat = safKlient.dokumentoversiktFagsak(query, projection);

        var journalposter = resultat.getJournalposter().stream()
            .filter(j -> j.getJournalstatus() == null || !EKSKLUDER_STATUS.contains(j.getJournalstatus()))
            .filter(j -> Journalposttype.U.equals(j.getJournalposttype()))
            .map(DokumentArkivTjeneste::mapTilArkivDokumentUtgående)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        return journalposter;
    }

    public Optional<ArkivDokumentUtgående> hentUtgåendeJournalpostForSak(JournalpostId journalpostId) {
        var query = new JournalpostQueryRequest();
        query.setJournalpostId(journalpostId.getVerdi());

        var resultat = safKlient.hentJournalpostInfo(query, utgåendeProjection());

        return Optional.ofNullable(resultat)
            .map(DokumentArkivTjeneste::mapTilArkivDokumentUtgående).orElse(List.of()).stream().findFirst();
    }

    private static JournalpostResponseProjection utgåendeProjection() {
        return new JournalpostResponseProjection()
            .journalpostId()
            .tittel()
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat()));
    }

    private static List<ArkivDokumentUtgående> mapTilArkivDokumentUtgående(Journalpost journalpost) {
        return Optional.ofNullable(journalpost.getDokumenter()).orElse(List.of()).stream()
            .filter(d -> d.getDokumentvarianter().stream().filter(Objects::nonNull).anyMatch(v -> Variantformat.ARKIV.equals(v.getVariantformat())))
            .map(d -> mapTilArkivDokumentUtgående(journalpost, d))
            .toList();
    }

    private static ArkivDokumentUtgående mapTilArkivDokumentUtgående(Journalpost journalpost, DokumentInfo dokumentInfo) {
        return new ArkivDokumentUtgående(journalpost.getTittel(), new JournalpostId(journalpost.getJournalpostId()), dokumentInfo.getDokumentInfoId());
    }

    public Set<DokumentTypeId> hentDokumentTypeIdForSak(Saksnummer saksnummer, LocalDate mottattEtterDato) {
        Set<DokumentTypeId> dokumenttyper = new HashSet<>();
        if (LocalDate.MIN.equals(mottattEtterDato)) {
            dokumenttyper.addAll(hentAlleJournalposterForSakSjekkCache(saksnummer).stream()
                .filter(ajp -> Kommunikasjonsretning.INN.equals(ajp.getKommunikasjonsretning()))
                .flatMap(jp -> ekstraherJournalpostDTID(jp).stream())
                .collect(Collectors.toSet()));
        } else {
            dokumenttyper.addAll(hentAlleJournalposterForSakSjekkCache(saksnummer).stream()
                .filter(ajp -> Kommunikasjonsretning.INN.equals(ajp.getKommunikasjonsretning()))
                .filter(jpost -> jpost.getTidspunkt() != null && jpost.getTidspunkt().isAfter(mottattEtterDato.atStartOfDay()))
                .flatMap(jp -> ekstraherJournalpostDTID(jp).stream())
                .collect(Collectors.toSet()));
        }
        dokumenttyper.addAll(DokumentTypeId.ekvivalenter(dokumenttyper));
        return dokumenttyper;
    }

    private Set<DokumentTypeId> ekstraherJournalpostDTID(ArkivJournalPost jpost) {
        Set<DokumentTypeId> alle = new HashSet<>();
        dokumentTypeFraTittel(jpost.getBeskrivelse()).ifPresent(alle::add);
        alle.addAll(ekstraherDokumentDTID(jpost.getHovedDokument()));
        jpost.getAndreDokument().forEach(dok -> alle.addAll(ekstraherDokumentDTID(dok)));
        return alle;
    }

    private Set<DokumentTypeId> ekstraherDokumentDTID(ArkivDokument dokument) {
        return Optional.ofNullable(dokument).map(ArkivDokument::getAlleDokumenttyper).orElse(Set.of());
    }

    private JournalpostResponseProjection standardJournalpostProjection() {
        return new JournalpostResponseProjection()
            .journalpostId()
            .journalposttype()
            .tittel()
            .journalstatus()
            .datoOpprettet()
            .tilleggsopplysninger(new TilleggsopplysningResponseProjection().nokkel().verdi())
            .dokumenter(new DokumentInfoResponseProjection()
                .dokumentInfoId()
                .tittel()
                .brevkode()
                .dokumentvarianter(new DokumentvariantResponseProjection().variantformat())
                .logiskeVedlegg(new LogiskVedleggResponseProjection().tittel()));
    }

    private ArkivJournalPost mapTilArkivJournalPost(Journalpost journalpost) {

        var dokumenter = journalpost.getDokumenter().stream()
            .map(this::mapTilArkivDokument)
            .collect(Collectors.toList());

        var doktypeFraTilleggsopplysning = Optional.ofNullable(journalpost.getTilleggsopplysninger()).orElse(List.of()).stream()
            .filter(to -> FP_DOK_TYPE.equals(to.getNokkel()))
            .map(to -> DokumentTypeId.finnForKodeverkEiersKode(to.getVerdi()))
            .collect(Collectors.toSet());
        var doktypeFraDokumenter = dokumenter.stream().map(ArkivDokument::getDokumentType).collect(Collectors.toSet());
        var alleTyper = new HashSet<>(doktypeFraDokumenter);
        alleTyper.addAll(doktypeFraTilleggsopplysning);
        if (!doktypeFraTilleggsopplysning.isEmpty() && !doktypeFraDokumenter.containsAll(doktypeFraTilleggsopplysning)) {
            LOG.info("DokArkivTjenest ulike dokumenttyper fra dokument {} fra tilleggsopplysning {}", doktypeFraDokumenter, doktypeFraTilleggsopplysning);
        } else if (doktypeFraTilleggsopplysning.isEmpty()) {
            LOG.info("DokArkivTjenest journalpost {} uten tilleggsopplysninger", journalpost.getJournalpostId());
        }
        var hoveddokumentType = utledHovedDokumentType(alleTyper);
        var hoveddokument = dokumenter.stream().filter(d -> hoveddokumentType.equals(d.getDokumentType())).findFirst();

        var builder = ArkivJournalPost.Builder.ny()
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
        var varianter = dokumentInfo.getDokumentvarianter().stream()
            .filter(Objects::nonNull)
            .map(Dokumentvariant::getVariantformat)
            .map(Variantformat::name)
            .map(VariantFormat::finnForKodeverkEiersKode)
            .collect(Collectors.toSet());

        return ArkivDokument.Builder.ny()
            .medDokumentId(dokumentInfo.getDokumentInfoId())
            .medTittel(dokumentInfo.getTittel())
            .medVariantFormater(varianter)
            .medAlleDokumenttyper(alleDokumenttyper)
            .medDokumentTypeId(utledHovedDokumentType(alleDokumenttyper))
            .build();
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

    private Optional<DokumentTypeId> dokumentTypeFraTittel(String tittel) {
        return Optional.ofNullable(tittel).map(DokumentTypeId::finnForKodeverkEiersNavn);
    }
}
