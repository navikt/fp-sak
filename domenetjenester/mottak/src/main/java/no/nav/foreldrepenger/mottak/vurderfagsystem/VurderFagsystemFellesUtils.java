package no.nav.foreldrepenger.mottak.vurderfagsystem;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VURDER_INFOTRYGD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class VurderFagsystemFellesUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VurderFagsystemFellesUtils.class);

    private static final TemporalAmount UKER_FH_SAMME = Period.ofWeeks(5);
    private static final TemporalAmount UKER_FH_ULIK = Period.ofWeeks(19);
    private static final Period MAKS_AVVIK_DAGER_IM_INPUT = Period.of(0,3,1);
    private static final Period OPPLYSNINGSPLIKT_INTERVALL = Period.of(0,2,1);
    private static final Period PERIODE_FOR_AKTUELLE_SAKER = Period.ofMonths(10);

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    public VurderFagsystemFellesUtils(){
        //Injected normal scoped bean is now proxyable
    }

    @Inject
    public VurderFagsystemFellesUtils(BehandlingRepositoryProvider repositoryProvider, MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                      InntektsmeldingTjeneste inntektsmeldingTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    public List<Fagsak> filtrerSakerForBehandlingTema(List<Fagsak> saker, BehandlingTema behandlingTema) {
        if (BehandlingTema.ikkeSpesifikkHendelse(behandlingTema)) {
            return saker;
        }
        return saker.stream()
            .filter(s -> behandlingTema.erKompatibelMed(this.getBehandlingsTemaForFagsak(s)))
            .collect(Collectors.toList());
    }

    public List<Fagsak> finnÅpneSaker(List<Fagsak> saker) {
        return saker.stream()
            .filter(Fagsak::erÅpen)
            .filter(s -> FagsakStatus.LØPENDE.equals(s.getStatus()) || harÅpenYtelsesBehandling(s))
            .collect(Collectors.toList());
    }

    private boolean harÅpenYtelsesBehandling(Fagsak fagsak) {
        return !behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).isEmpty();
    }

    private List<Fagsak> harSakMedAvslagGrunnetManglendeDok(List<Fagsak> saker) {
        return saker.stream()
            .filter(s -> mottatteDokumentTjeneste.erSisteYtelsesbehandlingAvslåttPgaManglendeDokumentasjon(s) && !mottatteDokumentTjeneste.harFristForInnsendingAvDokGåttUt(s))
            .collect(Collectors.toList());
    }

    public Optional<FamilieHendelseEntitet> finnGjeldendeFamilieHendelse(Fagsak fagsak) {
        return behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(behandling -> familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon));
    }

    public boolean erBehandlingAvsluttetFørOpplysningspliktIntervall(Behandling behandling) {
        return behandling.getAvsluttetDato() != null && behandling.getAvsluttetDato().isAfter(LocalDateTime.now().minus(OPPLYSNINGSPLIKT_INTERVALL));
    }

    public boolean harSakOpprettetInnenIntervall(List<Fagsak> sakerGittYtelseType) {
        return sakerGittYtelseType.stream()
            .anyMatch(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(LocalDateTime.now().minus(PERIODE_FOR_AKTUELLE_SAKER)));
    }

    public boolean erFagsakMedFamilieHendelsePassendeForFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Finn behandling
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId())
            .flatMap(b -> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()));
        if (fhGrunnlag.isEmpty()) {
            return false;
        }
        FamilieHendelseType fhType = fhGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        BehandlingTema bhTemaFagsak = BehandlingTema.fraFagsakHendelse(fagsak.getYtelseType(), fhType);
        if (!vurderFagsystem.getBehandlingTema().erKompatibelMed(bhTemaFagsak)) {
            return false;
        }

        // Sjekk familiehendelse
        if (FamilieHendelseType.gjelderFødsel(fhType)) {
            return erPassendeFødselsSak(vurderFagsystem, fhGrunnlag.get());
        } else if (FamilieHendelseType.gjelderAdopsjon(fhType)) {
            return erPassendeAdopsjonsSak(vurderFagsystem, fhGrunnlag.get());
        }
        return false;
    }

    public boolean erFagsakPassendeForFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Vurder omskriving av denne og neste til Predicate<Fagsak> basert på bruksmønster
        // Finn behandling
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId());
        if (behandling.isEmpty()) {
            return true;
        }

        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = behandling.flatMap(b -> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()));
        if (fhGrunnlag.isEmpty()) {
            // Her har vi en sak m/behandling uten FH - 3 hovedtilfelle uregistrert papirsøknad, infobrev far, im før søknad.
            // Innkommende kan være søknad, IM, eller ustrukturert For ES godtar man alt.
            return kanFagsakUtenGrunnlagBrukesForDokument(vurderFagsystem, behandling.get());
        }
        FamilieHendelseType fhType = fhGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon).map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        BehandlingTema bhTemaFagsak = BehandlingTema.fraFagsakHendelse(fagsak.getYtelseType(), fhType);
        if (!vurderFagsystem.getBehandlingTema().erKompatibelMed(bhTemaFagsak)) {
            return false;
        }

        // Sjekk familiehendelse
        if (FamilieHendelseType.gjelderFødsel(fhType)) {
            return erPassendeFødselsSak(vurderFagsystem, fhGrunnlag.get());
        } else if (FamilieHendelseType.gjelderAdopsjon(fhType)) {
            return erPassendeAdopsjonsSak(vurderFagsystem, fhGrunnlag.get());
        }
        return false;
    }

    public BehandlendeFagsystem vurderAktuelleFagsakerForInntektsmeldingFP(VurderFagsystem vurderFagsystem, List<Fagsak> saker) {
        var startdatoIM = vurderFagsystem.getStartDatoForeldrepengerInntektsmelding().orElse(null);
        if (startdatoIM == null) {
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        // Sorter saker etter om de har søknad, tidligere IM eller ikke + matching. Evaluer i rekkefølge
        Map<SorteringSaker, List<Fagsak>> sorterteSaker = saker.stream()
            .collect(Collectors.groupingBy(f -> sorterFagsakForStartdatoIM(f, startdatoIM)));

        if (!sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_DATO_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.GRUNNLAG_DATO_MATCH), SorteringSaker.GRUNNLAG_DATO_MATCH, vurderFagsystem);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_MULIG_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.GRUNNLAG_MULIG_MATCH), SorteringSaker.GRUNNLAG_MULIG_MATCH, vurderFagsystem);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_MISMATCH, List.of()).isEmpty() && harSakOpprettetInnenIntervall(sorterteSaker.get(SorteringSaker.GRUNNLAG_MISMATCH))) {
            LOG.info("VurderFagsystem FP IM manuell pga nylige saker av type {} for {}", SorteringSaker.GRUNNLAG_MISMATCH, vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.INNTEKTSMELDING_DATO_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.INNTEKTSMELDING_DATO_MATCH), SorteringSaker.INNTEKTSMELDING_DATO_MATCH, vurderFagsystem);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.INNTEKTSMELDING_MISMATCH, List.of()).isEmpty() && harSakOpprettetInnenIntervall(sorterteSaker.get(SorteringSaker.INNTEKTSMELDING_MISMATCH))) {
            LOG.info("VurderFagsystem FP IM manuell pga nylige saker av type {} for {}", SorteringSaker.INNTEKTSMELDING_MISMATCH, vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        return sorterteSaker.getOrDefault(SorteringSaker.TOM_SAK, List.of()).isEmpty() ?  new BehandlendeFagsystem(VURDER_INFOTRYGD) :
            new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(sorterteSaker.get(SorteringSaker.TOM_SAK).get(0).getSaksnummer());
    }

    private BehandlendeFagsystem behandlendeFagsystemFraFagsaker(List<Fagsak> saker, SorteringSaker sortering, VurderFagsystem vurderFagsystem) {
        if (saker.size() == 1) {
            return new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(saker.get(0).getSaksnummer());
        }
        if (saker.size() > 1) {
            LOG.info("VurderFagsystem FP IM manuell pga flere saker av type {} for {}", sortering, vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        throw new IllegalArgumentException("Utviklerfeil skal ikke kalles med tom liste");
    }

    private SorteringSaker sorterFagsakForStartdatoIM(Fagsak fagsak, LocalDate startdatoIM) {
        Behandling behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsak.getId()).stream()
            .findFirst().orElseGet(() -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null));
        if (behandling == null) {
            return SorteringSaker.TOM_SAK;
        }
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId());
        if (fhGrunnlag.isEmpty()) {
            Map<Arbeidsgiver, List<Inntektsmelding>> alleInntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsakInkludertInaktive(behandling.getFagsak().getSaksnummer());
            var match = alleInntektsmeldinger.values().stream().flatMap(Collection::stream).map(Inntektsmelding::getStartDatoPermisjon).flatMap(Optional::stream)
                .anyMatch(d -> startdatoIM.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(d) && startdatoIM.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(d));
            if (match) {
                return SorteringSaker.INNTEKTSMELDING_DATO_MATCH;
            }
            return alleInntektsmeldinger.isEmpty() ?  SorteringSaker.TOM_SAK : SorteringSaker.INNTEKTSMELDING_MISMATCH;
        }
        // Her har vi registrert søknad
        LocalDate førsteDagBehandling = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getFørsteUttaksdato();
        if (førsteDagBehandling.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(startdatoIM) && førsteDagBehandling.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(startdatoIM)) {
            return SorteringSaker.GRUNNLAG_DATO_MATCH;
        } else if ((fagsak.erÅpen() && harSakOpprettetInnenIntervall(List.of(fagsak))) || erBehandlingAvsluttetFørOpplysningspliktIntervall(behandling)) {
            return SorteringSaker.GRUNNLAG_MULIG_MATCH;
        } else {
            return SorteringSaker.GRUNNLAG_MISMATCH;
        }
    }

    /**
     * Scenario: Fagsak vurdert mangler FHgrunnlag (papirsøknad, IM, infobrev) mens incoming er søknad, im, ustrukturert
     * Logikk: bruk saken med mindre det allerede ligger IM med helt annen startdato der ift innkommende søknad/im
     */
    private boolean kanFagsakUtenGrunnlagBrukesForDokument(VurderFagsystem vurderFagsystem, Behandling sisteBehandling) {
        if (vurderFagsystem.getStartDatoForeldrepengerInntektsmelding().isPresent()) {
            LocalDate oppgittFørsteDag = vurderFagsystem.getStartDatoForeldrepengerInntektsmelding().get(); // NOSONAR
            Map<Arbeidsgiver, List<Inntektsmelding>> alleInntektsmeldinger = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsakInkludertInaktive(sisteBehandling.getFagsak().getSaksnummer());
            if (alleInntektsmeldinger.isEmpty()) {
                return true;
            }
            return alleInntektsmeldinger.values().stream().flatMap(Collection::stream).map(Inntektsmelding::getStartDatoPermisjon).flatMap(Optional::stream)
                .anyMatch(d -> oppgittFørsteDag.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(d) && oppgittFørsteDag.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(d));
        }
        return true;
    }

    public Optional<BehandlendeFagsystem> standardUstrukturertDokumentVurdering(List<Fagsak> sakerTilVurdering) {
        List<Fagsak> åpneFagsaker = finnÅpneSaker(sakerTilVurdering);
        if (åpneFagsaker.size() == 1) {
            return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(åpneFagsaker.get(0).getSaksnummer()));
        } else if (åpneFagsaker.size() > 1) {
            return Optional.of(new BehandlendeFagsystem(MANUELL_VURDERING));
        }
        List<Fagsak> avslagDokumentasjon = harSakMedAvslagGrunnetManglendeDok(sakerTilVurdering);
        if (avslagDokumentasjon.size() == 1) {
            return Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(avslagDokumentasjon.get(0).getSaksnummer()));
        }
        return Optional.empty();
    }

    public Optional<BehandlendeFagsystem> vurderFagsystemKlageAnke(List<Fagsak> sakerTilVurdering) {
        // Ruter inn på sak med nyeste vedtaksdato
        return sakerTilVurdering.stream()
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .filter(Objects::nonNull)
            .max(Comparator.comparing(b -> behandlingVedtakRepository.hentForBehandling(b.getId()).getVedtakstidspunkt()))
            .map(b -> new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(b.getFagsak().getSaksnummer()));
    }

    public static boolean erSøknad(VurderFagsystem vurderFagsystem) {
        return (DokumentTypeId.getSøknadTyper().contains(vurderFagsystem.getDokumentTypeId().getKode())) ||
            (DokumentKategori.SØKNAD.equals(vurderFagsystem.getDokumentKategori()));
    }

    private static boolean erPassendeFødselsSak(VurderFagsystem vurderFagsystem, FamilieHendelseGrunnlagEntitet grunnlag) {
        Optional<LocalDate> termindatoPåSak = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);
        Optional<LocalDate> fødselsdatoPåSak = grunnlag.getGjeldendeBarna().stream().map(UidentifisertBarn::getFødselsdato).findFirst();
        Optional<LocalDate> termindatoPåSøknad = vurderFagsystem.getBarnTermindato();
        Optional<LocalDate> fødselsdatoPåSøknad = vurderFagsystem.getBarnFodselsdato();

        return erDatoIPeriodeHvisBeggeErTilstede(termindatoPåSøknad, termindatoPåSak, UKER_FH_SAMME, UKER_FH_SAMME) ||
            erDatoIPeriodeHvisBeggeErTilstede(fødselsdatoPåSøknad, termindatoPåSak, UKER_FH_ULIK, UKER_FH_SAMME) ||
            erDatoIPeriodeHvisBeggeErTilstede(termindatoPåSøknad, fødselsdatoPåSak, UKER_FH_SAMME, UKER_FH_ULIK) ||
            erDatoIPeriodeHvisBeggeErTilstede(fødselsdatoPåSøknad, fødselsdatoPåSak, UKER_FH_SAMME, UKER_FH_SAMME);
    }

    private static boolean erPassendeAdopsjonsSak(VurderFagsystem vurderFagsystem, FamilieHendelseGrunnlagEntitet grunnlag) {
        Optional<LocalDate> overtagelsesDatoPåSak = grunnlag.getGjeldendeAdopsjon().map(AdopsjonEntitet::getOmsorgsovertakelseDato);
        Optional<LocalDate> overtagelsesDatoPåSøknad = vurderFagsystem.getOmsorgsovertakelsedato();

        if (!erDatoIPeriodeHvisBeggeErTilstede(overtagelsesDatoPåSøknad, overtagelsesDatoPåSak, UKER_FH_SAMME, UKER_FH_SAMME)) {
            return false;
        }

        List<LocalDate> fødselsDatoerPåSak = grunnlag.getGjeldendeBarna().stream().map(UidentifisertBarn::getFødselsdato)
            .collect(Collectors.toList());
        List<LocalDate> fødselsDatoerPåSøknad = vurderFagsystem.getAdopsjonsbarnFodselsdatoer();

        return erAdopsjonsBarnFødselsdatoerLike(fødselsDatoerPåSak, fødselsDatoerPåSøknad);
    }

    private static boolean erAdopsjonsBarnFødselsdatoerLike(List<LocalDate> datoer1, List<LocalDate> datoer2) {
        if (datoer1.size() != datoer2.size()) {
            return false;
        }
        List<LocalDate> d1 = new ArrayList<>(datoer1);
        List<LocalDate> d2 = new ArrayList<>(datoer2);
        Collections.sort(d1);
        Collections.sort(d2);

        for (int i = 0; i < d1.size(); i++) {
            if (!d1.get(i).equals(d2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean erDatoIPeriodeHvisBeggeErTilstede(Optional<LocalDate> nyDato, Optional<LocalDate> periodeDato, TemporalAmount førPeriode, TemporalAmount etterPeriode) {
        return (periodeDato.isPresent() && nyDato.isPresent())
            && !(nyDato.get().isBefore(periodeDato.get().minus(førPeriode)) || nyDato.get().isAfter(periodeDato.get().plus(etterPeriode)));
    }

    private BehandlingTema getBehandlingsTemaForFagsak(Fagsak s) {
        Optional<Behandling> behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(s.getId());
        if (behandling.isEmpty()) {
            return BehandlingTema.fraFagsakHendelse(s.getYtelseType(), FamilieHendelseType.UDEFINERT);
        }

        final FamilieHendelseType fhType = behandling.flatMap(b -> familieHendelseRepository.hentAggregatHvisEksisterer(b.getId()))
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getType).orElse(FamilieHendelseType.UDEFINERT);
        return BehandlingTema.fraFagsakHendelse(s.getYtelseType(), fhType);
    }

    public enum SorteringSaker {
        GRUNNLAG_DATO_MATCH,
        GRUNNLAG_MULIG_MATCH,
        GRUNNLAG_MISMATCH,
        INNTEKTSMELDING_DATO_MATCH,
        INNTEKTSMELDING_MISMATCH,
        TOM_SAK
    }

}
