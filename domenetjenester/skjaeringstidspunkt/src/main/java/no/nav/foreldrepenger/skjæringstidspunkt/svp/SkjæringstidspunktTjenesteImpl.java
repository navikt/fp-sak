package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static no.nav.foreldrepenger.skjæringstidspunkt.svp.BeregnTilrettleggingsdato.beregn;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktRegisterinnhentingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste, SkjæringstidspunktRegisterinnhentingTjeneste {

    private static final int MAX_SVANGERSKAP_UKER = 42;

    private SvangerskapspengerRepository svangerskapspengerRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    SkjæringstidspunktTjenesteImpl() {
        //CDI
    }

    @Inject
    public SkjæringstidspunktTjenesteImpl(SvangerskapspengerRepository svangerskapspengerRepository,
                                          BeregningsresultatRepository beregningsresultatRepository,
                                          FamilieHendelseRepository familieHendelseRepository,
                                          OpptjeningRepository opptjeningRepository,
                                          BehandlingRepository behandlingRepository) {
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkter(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var førsteUttakSøknad = førsteØnskedeUttaksdag(behandling);
        var skjæringstidspunkt = opptjeningRepository.finnOpptjening(behandlingId).map(o -> o.getTom().plusDays(1)).orElse(førsteUttakSøknad);

        return Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(førsteUttakSøknad)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttakSøknad)
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt)
            .medUtledetMedlemsintervall(utledYtelseintervall(behandlingId, førsteUttakSøknad))
            .build();
    }

    @Override
    public LocalDate utledSkjæringstidspunktForRegisterInnhenting(Long behandlingId) {
        return utledSkjæringstidspunktRegisterinnhenting(behandlingId);
    }

    private LocalDate førsteØnskedeUttaksdag(Behandling behandling) {
        var førsteUttakSøknad = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .map(this::utledBasertPåGrunnlag);

        if (behandling.erRevurdering()) {
            final var førsteUttaksdagIForrigeVedtak = finnFørsteDatoMedUttak(behandling);
            if (førsteUttaksdagIForrigeVedtak.isEmpty() && førsteUttakSøknad.isEmpty()) {
                return svangerskapspengerRepository.hentGrunnlag(originalBehandling(behandling))
                    .map(this::utledBasertPåGrunnlag)
                    .orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
            }
            final var skjæringstidspunkt = utledTidligste(førsteUttakSøknad.orElse(Tid.TIDENES_ENDE),
                førsteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_ENDE));
            if (skjæringstidspunkt.equals(Tid.TIDENES_ENDE)) {
                // Fant da ikke noe skjæringstidspunkt i tidligere vedtak heller.
                throw finnerIkkeStpException(behandling.getId());
            }
            return skjæringstidspunkt;
        }
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda så gir midlertidig dagens dato. for at DTOer skal fungere.
            return førsteUttakSøknad.orElse(LocalDate.now());
        }
        return førsteUttakSøknad.orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
    }

    private TekniskException finnerIkkeStpException(Long behandlingId) {
        return new TekniskException("FP-931233",
            "Finner ikke skjæringstidspunkt for svangerskapspenger som forventet for behandling=" + behandlingId);
    }

    LocalDate utledBasertPåGrunnlag(SvpGrunnlagEntitet grunnlag) {
        Optional<LocalDate> tidligsteTilretteleggingsDatoOpt = Optional.ofNullable(grunnlag.getOverstyrteTilrettelegginger())
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
            .flatMap(this::tidligsteDatoFraTIlrettelegging)
            .or(() -> Optional.ofNullable(grunnlag.getOpprinneligeTilrettelegginger())
                .map(SvpTilretteleggingerEntitet::getTilretteleggingListe)
                .flatMap(this::tidligsteDatoFraTIlrettelegging));
        return tidligsteTilretteleggingsDatoOpt.orElseThrow(() -> new IllegalStateException("Klarte ikke finne skjæringstidspunkt for SVP"));
    }

    Optional<LocalDate> tidligsteDatoFraTIlrettelegging(List<SvpTilretteleggingEntitet> tilrettelegginger) {
        if (tilrettelegginger == null || tilrettelegginger.isEmpty()) {
            return Optional.empty();
        }
        return tilrettelegginger.stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .map(aktuelle -> beregn(aktuelle.getBehovForTilretteleggingFom(),
            aktuelle.getTilretteleggingFOMListe().stream()
                .filter(tl -> tl.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING))
                .map(TilretteleggingFOM::getFomDato)
                .min(LocalDate::compareTo),
            aktuelle.getTilretteleggingFOMListe().stream()
                .filter(tl -> tl.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING))
                .map(TilretteleggingFOM::getFomDato)
                .min(LocalDate::compareTo),
            aktuelle.getTilretteleggingFOMListe().stream()
                .filter(tl -> tl.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING))
                .map(TilretteleggingFOM::getFomDato)
                .min(LocalDate::compareTo)))
            .min(Comparator.naturalOrder());
    }

    private Optional<LocalDate> finnFørsteDatoMedUttak(Behandling behandling) {
        var perioder = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(List.of());
        if (!finnesPerioderMedUtbetaling(perioder)) {
            return perioder.stream()
                .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
                .min(Comparator.naturalOrder());
        }
        return perioder.stream()
            .filter(it -> it.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
    }

    private Long originalBehandling(Behandling behandling) {
        return behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalArgumentException("Revurdering må ha original behandling"));
    }

    private boolean finnesPerioderMedUtbetaling(List<BeregningsresultatPeriode> perioder) {
        return perioder.stream().anyMatch(p -> p.getDagsats() > 0);
    }

    private LocalDate utledTidligste(LocalDate første, LocalDate andre) {
        return første.isBefore(andre) ? første :  andre;
    }

    private LocalDate utledSkjæringstidspunktRegisterinnhenting(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var svpGrunnlagOpt = svangerskapspengerRepository.hentGrunnlag(behandlingId)
            .or(() -> BehandlingType.REVURDERING.equals(behandling.getType()) ?
                svangerskapspengerRepository.hentGrunnlag(originalBehandling(behandling)) : Optional.empty());

        var tidligsteTilretteleggingsDatoOpt = svpGrunnlagOpt
            .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
            .min(Comparator.naturalOrder());

        return tidligsteTilretteleggingsDatoOpt
            .or(() -> opptjeningRepository.finnOpptjening(behandlingId).map(o -> o.getTom().plusDays(1)))
            .orElseGet(LocalDate::now);
    }

    private LocalDateInterval utledYtelseintervall(Long behandlingId, LocalDate skjæringstidspunkt) {
        try {
            var antattTom = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
                .orElse(skjæringstidspunkt.plusWeeks(MAX_SVANGERSKAP_UKER));
            return new LocalDateInterval(skjæringstidspunkt, antattTom);
        } catch (Exception e) {
            return new LocalDateInterval(skjæringstidspunkt, skjæringstidspunkt.plusWeeks(MAX_SVANGERSKAP_UKER));
        }
    }
}
