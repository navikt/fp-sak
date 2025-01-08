package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

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
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.skjæringstidspunkt.FamilieHendelseMapper;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.konfig.Tid;

@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class SkjæringstidspunktTjenesteImpl implements SkjæringstidspunktTjeneste {

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
        var førsteUttakSøknadOpt = Optional.ofNullable(førsteØnskedeUttaksdag(behandling));
        var førsteUttakSøknad = førsteUttakSøknadOpt.orElseGet(LocalDate::now); // Mangler grunnlag for å angi dato, bruker midlertidig dagens dato pga Dtos etc.
        var skjæringstidspunkt = opptjeningRepository.finnOpptjening(behandlingId)
            .filter(Opptjening::erOpptjeningPeriodeVilkårOppfylt)
            .map(o -> o.getTom().plusDays(1))
            .orElse(førsteUttakSøknad);

        var builder = Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(førsteUttakSøknad)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttakSøknad)
            .medFørsteUttaksdatoSøknad(førsteUttakSøknadOpt.orElse(null))
            .medUtledetSkjæringstidspunkt(skjæringstidspunkt)
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt)
            .medUttaksintervall(utledYtelseintervall(behandlingId, førsteUttakSøknad));
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        try {
            familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseMapper::mapTilFamilieHendelseDato)
                .ifPresent(builder::medFamilieHendelseDato);
        } catch (Exception e) {
            // Testformål
        }
        return builder.build();
    }

    @Override
    public Skjæringstidspunkt getSkjæringstidspunkterForAvsluttetBehandling(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var førsteUttak = finnFørsteDatoMedUttak(behandling).orElseThrow();
        var skjæringstidspunkt = opptjeningRepository.finnOpptjening(behandlingId)
            .filter(Opptjening::erOpptjeningPeriodeVilkårOppfylt)
            .map(o -> o.getTom().plusDays(1))
            .orElse(førsteUttak);

        var builder = Skjæringstidspunkt.builder()
            .medFørsteUttaksdato(førsteUttak)
            .medFørsteUttaksdatoGrunnbeløp(førsteUttak)
            .medFørsteUttaksdatoSøknad(førsteUttak)
            .medUtledetSkjæringstidspunkt(førsteUttak)
            .medSkjæringstidspunktOpptjening(skjæringstidspunkt)
            .medUttaksintervall(utledYtelseintervall(behandlingId, førsteUttak));
        var familieHendelseGrunnlag = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId);
        try {
            familieHendelseGrunnlag.map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(FamilieHendelseMapper::mapTilFamilieHendelseDato)
                .ifPresent(builder::medFamilieHendelseDato);
        } catch (Exception e) {
            // Testformål
        }
        return builder.build();
    }

    private LocalDate førsteØnskedeUttaksdag(Behandling behandling) {
        var førsteUttakSøknad = svangerskapspengerRepository.hentGrunnlag(behandling.getId())
            .map(SkjæringstidspunktTjenesteImpl::utledBasertPåGrunnlag);

        if (behandling.erRevurdering()) {
            var førsteUttaksdagIForrigeVedtak = finnFørsteDatoMedUttak(behandling);
            if (førsteUttaksdagIForrigeVedtak.isEmpty() && førsteUttakSøknad.isEmpty()) {
                return svangerskapspengerRepository.hentGrunnlag(originalBehandling(behandling))
                    .map(SkjæringstidspunktTjenesteImpl::utledBasertPåGrunnlag)
                    .orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
            }
            var skjæringstidspunkt = utledTidligste(førsteUttakSøknad.orElse(Tid.TIDENES_ENDE),
                førsteUttaksdagIForrigeVedtak.orElse(Tid.TIDENES_ENDE));
            if (skjæringstidspunkt.equals(Tid.TIDENES_ENDE)) {
                // Fant da ikke noe skjæringstidspunkt i tidligere vedtak heller.
                throw finnerIkkeStpException(behandling.getId());
            }
            return skjæringstidspunkt;
        }
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            // Har ikke grunnlag for å avgjøre skjæringstidspunkt enda
            return førsteUttakSøknad.orElse(null);
        }
        return førsteUttakSøknad.orElseThrow(() -> finnerIkkeStpException(behandling.getId()));
    }

    private TekniskException finnerIkkeStpException(Long behandlingId) {
        return new TekniskException("FP-931233",
            "Finner ikke skjæringstidspunkt for svangerskapspenger som forventet for behandling=" + behandlingId);
    }

    static LocalDate utledBasertPåGrunnlag(SvpGrunnlagEntitet grunnlag) {
        // Kan ikke bruke tilretteleggingfilter - kan hende at overstyrt gir tom dato, må da sjekke oppgitt
        return Optional.ofNullable(grunnlag.getOverstyrteTilrettelegginger())
            .flatMap(SkjæringstidspunktTjenesteImpl::tidligsteDatoFraTilrettelegginger)
            .or(() -> Optional.ofNullable(grunnlag.getOpprinneligeTilrettelegginger())
                .flatMap(SkjæringstidspunktTjenesteImpl::tidligsteDatoFraTilrettelegginger))
            .orElseThrow(() -> new IllegalStateException("Klarte ikke finne skjæringstidspunkt for SVP"));
    }

    private static Optional<LocalDate> tidligsteDatoFraTilrettelegginger(SvpTilretteleggingerEntitet tilrettelegginger) {
        return Optional.ofNullable(tilrettelegginger)
            .map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(List.of()).stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .map(BeregnTilrettleggingsdato::beregnFraTilrettelegging)
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

    private LocalDateInterval utledYtelseintervall(Long behandlingId, LocalDate skjæringstidspunkt) {
        try {
            var antattTom = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
                .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                .map(SkjæringstidspunktTjenesteImpl::datoYtelsenOpphører)
                .orElse(skjæringstidspunkt.plusWeeks(MAX_SVANGERSKAP_UKER));
            return new LocalDateInterval(skjæringstidspunkt, antattTom);
        } catch (Exception e) {
            return new LocalDateInterval(skjæringstidspunkt, skjæringstidspunkt.plusWeeks(MAX_SVANGERSKAP_UKER));
        }
    }

    private static LocalDate datoYtelsenOpphører(FamilieHendelseEntitet familieHendelse) {
        var fraTermin = familieHendelse.getTermindato().map(t -> t.minusWeeks(3)).orElse(null);
        var fraFødsel = familieHendelse.getFødselsdato().orElse(null);
        if (fraFødsel != null && fraTermin != null) {
            return fraFødsel.isBefore(fraTermin) ? fraFødsel : fraTermin;
        } else {
            return fraFødsel != null ? fraFødsel : fraTermin;
        }
    }
}
