package no.nav.foreldrepenger.mottak.vurderfagsystem;

import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING;
import static no.nav.foreldrepenger.behandling.BehandlendeFagsystem.BehandlendeSystem.VURDER_INFOTRYGD;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Collection;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentKategori;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class VurderFagsystemFellesUtils {

    private static final Logger LOG = LoggerFactory.getLogger(VurderFagsystemFellesUtils.class);

    private static final Period MAKS_AVVIK_DAGER_IM_INPUT = Period.of(0,3,1);
    private static final Period OPPLYSNINGSPLIKT_INTERVALL = Period.of(0,2,1);
    private static final Period PERIODE_FOR_AKTUELLE_SAKER = Period.ofMonths(10);
    private static final Period PERIODE_FOR_AKTUELLE_SAKER_IM = Period.ofMonths(10);
    private static final Period PERIODE_FOR_MULIGE_SAKER_IM = Period.ofMonths(14);

    private BehandlingRepository behandlingRepository;
    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;

    public VurderFagsystemFellesUtils(){
        //Injected normal scoped bean is now proxyable
    }

    @Inject
    public VurderFagsystemFellesUtils(BehandlingRepositoryProvider repositoryProvider, FamilieHendelseTjeneste familieHendelseTjeneste,
                                      MottatteDokumentTjeneste mottatteDokumentTjeneste,
                                      InntektsmeldingTjeneste inntektsmeldingTjeneste, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseTjeneste = familieHendelseTjeneste;
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
        return behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(behandling -> familieHendelseTjeneste.finnAggregat(behandling.getId())
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon));
    }

    public boolean erBehandlingAvsluttetFørOpplysningspliktIntervall(Behandling behandling) {
        return behandling.getAvsluttetDato() != null && behandling.getAvsluttetDato().isAfter(LocalDateTime.now().minus(OPPLYSNINGSPLIKT_INTERVALL));
    }

    public boolean harSakOpprettetInnenIntervall(List<Fagsak> sakerGittYtelseType) {
        var sammenlign = LocalDateTime.now().minus(PERIODE_FOR_AKTUELLE_SAKER);
        return sakerGittYtelseType.stream()
            .anyMatch(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(sammenlign));
    }

    public boolean harSakOpprettetInnenTvilsintervall(List<Fagsak> sakerGittYtelseType) {
        var sammenlign = LocalDateTime.now().minus(PERIODE_FOR_MULIGE_SAKER_IM);
        return sakerGittYtelseType.stream()
            .anyMatch(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(sammenlign));
    }

    public boolean harSakOpprettetInnenIntervallForIM(List<Fagsak> sakerGittYtelseType, LocalDate referanseDato) {
        var sammenlign = referanseDato.atStartOfDay().minus(PERIODE_FOR_AKTUELLE_SAKER_IM);
        return sakerGittYtelseType.stream()
            .anyMatch(f -> f.getOpprettetTidspunkt() != null && f.getOpprettetTidspunkt().isAfter(sammenlign));
    }

    public boolean erFagsakMedFamilieHendelsePassendeForFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak) {
        // Finn behandling
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId())
            .flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId()));
        if (fhGrunnlag.isEmpty()) {
            return false;
        }
        return erGrunnlagPassendeFor(fhGrunnlag.get(), fagsak.getYtelseType(), vurderFagsystem);
    }

    public boolean erFagsakPassendeForFamilieHendelse(VurderFagsystem vurderFagsystem, Fagsak fagsak, boolean vurderHenlagte) {
        // Vurder omskriving av denne og neste til Predicate<Fagsak> basert på bruksmønster
        // Finn behandling
        Optional<Behandling> behandling = vurderHenlagte ? behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()) :
            behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(fagsak.getId());
        if (behandling.isEmpty()) {
            return true;
        }

        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = behandling.flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId()));
        if (fhGrunnlag.isEmpty()) {
            // Her har vi en sak m/behandling uten FH - 3 hovedtilfelle uregistrert papirsøknad, infobrev far, im før søknad.
            // Innkommende kan være søknad, IM, eller ustrukturert For ES godtar man alt.
            return kanFagsakUtenGrunnlagBrukesForDokument(vurderFagsystem, behandling.get());
        }
        return erGrunnlagPassendeFor(fhGrunnlag.get(), fagsak.getYtelseType(), vurderFagsystem);
    }

    private boolean erGrunnlagPassendeFor(FamilieHendelseGrunnlagEntitet grunnlag, FagsakYtelseType ytelseType, VurderFagsystem vurderFagsystem) {
        FamilieHendelseType fhType = grunnlag.getGjeldendeVersjon().getType();
        BehandlingTema bhTemaFagsak = BehandlingTema.fraFagsakHendelse(ytelseType, fhType);
        if (!vurderFagsystem.getBehandlingTema().erKompatibelMed(bhTemaFagsak)) {
            return false;
        }
        // Sjekk familiehendelse
        if (FamilieHendelseType.gjelderFødsel(fhType)) {
            return familieHendelseTjeneste.matcherFødselsSøknadMedBehandling(grunnlag,
                vurderFagsystem.getBarnTermindato().orElse(null), vurderFagsystem.getBarnFodselsdato().orElse(null));
        } else if (FamilieHendelseType.gjelderAdopsjon(fhType)) {
            return familieHendelseTjeneste.matcherOmsorgsSøknadMedBehandling(grunnlag,
                vurderFagsystem.getOmsorgsovertakelsedato().orElse(null), vurderFagsystem.getAdopsjonsbarnFodselsdatoer());
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
        if (!sorterteSaker.getOrDefault(SorteringSaker.GRUNNLAG_MISMATCH, List.of()).isEmpty() && harSakOpprettetInnenTvilsintervall(sorterteSaker.get(SorteringSaker.GRUNNLAG_MISMATCH))) {
            LOG.info("VurderFagsystem FP IM manuell pga nylige saker av type {} for {}", SorteringSaker.GRUNNLAG_MISMATCH, vurderFagsystem.getAktørId());
            return new BehandlendeFagsystem(MANUELL_VURDERING);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.INNTEKTSMELDING_DATO_MATCH, List.of()).isEmpty()) {
            return behandlendeFagsystemFraFagsaker(sorterteSaker.get(SorteringSaker.INNTEKTSMELDING_DATO_MATCH), SorteringSaker.INNTEKTSMELDING_DATO_MATCH, vurderFagsystem);
        }
        if (!sorterteSaker.getOrDefault(SorteringSaker.INNTEKTSMELDING_MISMATCH, List.of()).isEmpty() && harSakOpprettetInnenTvilsintervall(sorterteSaker.get(SorteringSaker.INNTEKTSMELDING_MISMATCH))) {
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
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = familieHendelseTjeneste.finnAggregat(behandling.getId());
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
        var referanseDatoForSaker = LocalDate.now().isBefore(startdatoIM) ? LocalDate.now() : startdatoIM;
        if (førsteDagBehandling.minus(MAKS_AVVIK_DAGER_IM_INPUT).isBefore(startdatoIM) && førsteDagBehandling.plus(MAKS_AVVIK_DAGER_IM_INPUT).isAfter(startdatoIM)) {
            return SorteringSaker.GRUNNLAG_DATO_MATCH;
        } else if ((fagsak.erÅpen() && harSakOpprettetInnenIntervallForIM(List.of(fagsak), referanseDatoForSaker)) || erBehandlingAvsluttetFørOpplysningspliktIntervall(behandling)) {
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
        var behandlinger = sakerTilVurdering.stream()
            .flatMap(f -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(f.getId()).stream())
            .filter(Objects::nonNull)
            .filter(b -> behandlingVedtakRepository.hentForBehandling(b.getId()).getVedtakstidspunkt().isAfter(LocalDateTime.now().minusYears(2)))
            .collect(Collectors.toList());
        if (behandlinger.isEmpty() && sakerTilVurdering.isEmpty()) {
            LOG.info("VFS-KLAGE ingen saker i VL");
        } else if (behandlinger.isEmpty()) {
            var saker = sakerTilVurdering.stream().map(Fagsak::getSaksnummer).collect(Collectors.toList());
            var sakerMedÅpenBehandling = sakerTilVurdering.stream().filter(f -> !behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(f.getId()).isEmpty()).count();
            LOG.info("VFS-KLAGE ingen vedtak - antall saker {} saker med åpen behandling {} saksnummer {}", sakerTilVurdering.size(), sakerMedÅpenBehandling, saker);
        } else if (behandlinger.size() > 1) {
            var saker = sakerTilVurdering.stream().map(Fagsak::getSaksnummer).collect(Collectors.toList());
            var sakerMedKlage = sakerTilVurdering.stream().filter(f -> behandlingRepository.hentSisteBehandlingAvBehandlingTypeForFagsakId(f.getId(), BehandlingType.KLAGE).isPresent()).count();
            LOG.info("VFS-KLAGE flere saker - antall saker {} saker med klage {} saksnummer {}", sakerTilVurdering.size(), sakerMedKlage, saker);
        }
        return behandlinger.size() != 1 ? Optional.empty() :
            Optional.of(new BehandlendeFagsystem(VEDTAKSLØSNING).medSaksnummer(behandlinger.get(0).getFagsak().getSaksnummer()));
    }

    public static boolean erSøknad(VurderFagsystem vurderFagsystem) {
        return (DokumentTypeId.getSøknadTyper().contains(vurderFagsystem.getDokumentTypeId())) ||
            (DokumentKategori.SØKNAD.equals(vurderFagsystem.getDokumentKategori()));
    }

    private BehandlingTema getBehandlingsTemaForFagsak(Fagsak s) {
        Optional<Behandling> behandling = behandlingRepository.finnSisteIkkeHenlagteYtelseBehandlingFor(s.getId());
        if (behandling.isEmpty()) {
            return BehandlingTema.fraFagsakHendelse(s.getYtelseType(), FamilieHendelseType.UDEFINERT);
        }

        final FamilieHendelseType fhType = behandling.flatMap(b -> familieHendelseTjeneste.finnAggregat(b.getId()))
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
