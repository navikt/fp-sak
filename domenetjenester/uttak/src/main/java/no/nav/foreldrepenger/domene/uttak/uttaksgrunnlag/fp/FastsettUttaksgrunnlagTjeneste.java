package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.finnesOverlapp;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.OppgittPeriodeUtil.slåSammenLikePerioder;
import static no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp.VedtaksperioderHelper.klipp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelser;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

@Dependent
public class FastsettUttaksgrunnlagTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FastsettUttaksgrunnlagTjeneste.class);
    private final FpUttakRepository fpUttakRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private final EndringsdatoFørstegangsbehandlingUtleder endringsdatoFørstegangsbehandlingUtleder;
    private final EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder;

    @Inject
    public FastsettUttaksgrunnlagTjeneste(UttakRepositoryProvider provider,
                                          EndringsdatoFørstegangsbehandlingUtleder endringsdatoFørstegangsbehandlingUtleder,
                                          @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder) {
        this.fpUttakRepository = provider.getFpUttakRepository();
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
        this.uttaksperiodegrenseRepository = provider.getUttaksperiodegrenseRepository();
        this.endringsdatoFørstegangsbehandlingUtleder = endringsdatoFørstegangsbehandlingUtleder;
        this.endringsdatoRevurderingUtleder = endringsdatoRevurderingUtleder;
    }

    public void fastsettUttaksgrunnlag(UttakInput input) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(input.getBehandlingReferanse().behandlingId());
        var eksisterendeJustertFordeling = ytelseFordelingAggregat.getJustertFordeling().orElse(null);
        var eksisterendeEndringsdato = ytelseFordelingAggregat.getAvklarteDatoer().map(AvklarteUttakDatoerEntitet::getOpprinneligEndringsdato).orElse(LocalDate.MIN);

        var endringsdatoRevurdering = utledEndringsdatoVedRevurdering(input);
        var justertFordeling = justerFordeling(input, endringsdatoRevurdering);
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        //Endringsdato skal utledes før justering ved revurdering, men etter justering for førstegangsbehandlinger
        LocalDate endringsdato;
        if (input.getBehandlingReferanse().erRevurdering()) {
            endringsdato = endringsdatoRevurdering;
        } else {
            endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(input.getBehandlingReferanse().behandlingId(),
                    justertFordeling.getPerioder());
        }
        LOG.info("Utledet endringsdato {}", endringsdato);

        if (!SammenlignFordeling.erLikeFordelinger(eksisterendeJustertFordeling, justertFordeling) || endringsdato == null || !eksisterendeEndringsdato.isEqual(endringsdato)) {
            var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId);
            var avklarteUttakDatoer = avklarteDatoerMedEndringsdato(behandlingId, endringsdato);
            yfBuilder.medJustertFordeling(justertFordeling)
                .medAvklarteDatoer(avklarteUttakDatoer)
                .medOverstyrtFordeling(null);
            ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
        }
    }

    private OppgittFordelingEntitet justerFordeling(UttakInput input, LocalDate endringsdatoRevurdering) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var fordeling = ytelseFordelingAggregat.getOppgittFordeling();
        var justertePerioder = getSøknadsPerioderOppdatertMedMottattDato(input, ytelseFordelingAggregat);
        if (ref.erRevurdering()) {
            var originalBehandlingId = ref.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalArgumentException("Utvikler-feil: ved revurdering skal det alltid finnes en original behandling"));
            if (behandlingHarUttaksresultat(originalBehandlingId)) {
                justertePerioder = fjernPerioderFørEndringsdato(justertePerioder, endringsdatoRevurdering);
                justertePerioder = kopierVedtaksperioderFomEndringsdato(justertePerioder, endringsdatoRevurdering, originalBehandlingId);
            } else {
                justertePerioder = oppgittePerioderFraForrigeBehandling(originalBehandlingId);
            }
        }

        if (skalJustereFordelingEtterFamiliehendelse(input, justertePerioder)) {
            justertePerioder = justerFordelingEtterFamilieHendelse(input.getYtelsespesifiktGrunnlag(), justertePerioder, ref.relasjonRolle(), fordeling.ønskerJustertVedFødsel());
            LOG.info("Justerte perioder etter flytting ved endring i familiehendelse {}", justertePerioder);
        }
        justertePerioder = slåSammenLikePerioder(justertePerioder);

        justertePerioder = fjernOppholdsperioderFriEllerLiggendeTilSlutt(justertePerioder);

        justertePerioder = leggTilUtsettelserForPleiepenger(input, justertePerioder);
        return new OppgittFordelingEntitet(kopier(justertePerioder), fordeling.getErAnnenForelderInformert(), fordeling.ønskerJustertVedFødsel());
    }

    private static List<OppgittPeriodeEntitet> fjernPerioderFørEndringsdato(List<OppgittPeriodeEntitet> oppgittePerioder, LocalDate endringsdato) {
        //Kan skje feks ved nytt pleiepenger vedtak på behandling opprettet pga ny IM (behandling som i utgangspunktet er tiltenkt å hoppe over uttak).
        //Oppgitt fordeling er i disse tilfellene kopiert fra tidligere behandling
        if (oppgittePerioder.stream().anyMatch(op -> op.getFom().isBefore(endringsdato))) {
            LOG.info("Fant og filtrerer ut oppgitte perioder før endringsdato {}", endringsdato);
        }
        return oppgittePerioder.stream()
            .flatMap(op -> klipp(op, endringsdato, Optional.empty()))
            .filter(op -> !op.getFom().isBefore(endringsdato))
            .toList();
    }

    private static boolean skalJustereFordelingEtterFamiliehendelse(UttakInput input, List<OppgittPeriodeEntitet> perioder) {
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        if (input.getBehandlingReferanse().behandlingId().equals(3291969L)) {
            return false;
        }

        if (!fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            return false;
        }

        if (perioder.isEmpty()) {
            LOG.info("Skal ikke fødselsjustere når gjeldende behandling ikke har uttak (f.eks. ved opphør, berørt, osv)");
            return false;
        }

        if (finnesOverlapp(perioder)) {
            LOG.warn("Finnes overlapp i oppgitte perioder fra søknad. Sannsynligvis feil i søknadsdialogen. "
                + "Hvis periodene ikke kan slås sammen faller behandlingen ut til manuell behandling");
            return false;
        }

        if (fpGrunnlag.getOriginalBehandling().isPresent()) {
            if (nySøknadPåTermin(fpGrunnlag.getFamilieHendelser())) {
                LOG.info("Skal ikke fødselsjustere ny søknad på termin");
                return false;
            }

            var gjeldendeFamilieHendelseOriginalBehandling = fpGrunnlag.getOriginalBehandling().get().getFamilieHendelser().getGjeldendeFamilieHendelse();
            var gjeldendeFamilieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
            if (gjeldendeFamilieHendelseOriginalBehandling.getFødselsdato().isEmpty() &&
                input.getBehandlingÅrsaker().contains(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) &&
                gjeldendeFamilieHendelse.getFødselsdato().isPresent()) {
                var mottattDato = perioder.stream()
                    .filter(p -> p.getMottattDato() != null)
                    .min(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                    .orElseThrow()
                    .getMottattDato();
                var fødselsdato = gjeldendeFamilieHendelse.getFødselsdato().get();

                if (mottattDato.isAfter(fødselsdato)) {
                    // TODO: return false? Ikke juster disse tilfellene?
                    LOG.info(
                        "Termin til fødsel: Original behandling ble søkt på termin, mens ny førstegangssøknad (med fødsel) er sendt inn ETTER registert fødsel {} med mottattdato {}",
                        fødselsdato, mottattDato);
                } else {
                    LOG.info(
                        "Termin til fødsel: Original behandling ble søkt på termin, mens ny førstegangssøknad (med fødsel) er sendt inn samme dato eller før registret fødsel {} med mottattdato {}",
                        fødselsdato, mottattDato);
                }

                var førsteUttaksperiode = perioder.stream().min(Comparator.comparing(OppgittPeriodeEntitet::getFom)).orElseThrow();
                if (fødselsdato.isEqual(førsteUttaksperiode.getFom())) {
                    LOG.info("Termin til fødsel: Startdato for første uttaksperiode er lik fødseldato {}", fødselsdato);
                } else if (førsteUttaksperiode.getFom().isBefore(fødselsdato)) {
                    LOG.info("Termin til fødsel: Startdato for første uttaksperiode er før fødseldato {}", fødselsdato);
                } else {
                    LOG.info("Termin til fødsel: Startdato for første uttaksperiode er etter fødseldato {}", fødselsdato);
                }
                // TODO: Utled om en skal justere eller ikke (per nå justere vi)
            }
        }

        return true;
    }

    private List<OppgittPeriodeEntitet> leggTilUtsettelserForPleiepenger(UttakInput input, List<OppgittPeriodeEntitet> perioder) {
        return PleiepengerJustering.juster(input.getBehandlingReferanse().aktørId(), input.getIayGrunnlag(), perioder);
    }

    private List<OppgittPeriodeEntitet> fjernOppholdsperioderFriEllerLiggendeTilSlutt(List<OppgittPeriodeEntitet> perioder) {
        var perioderUtenFriOpphold = perioder.stream()
            .filter(p -> !p.isOpphold() || UtsettelseCore2021.kreverSammenhengendeUttak(p))
            .toList();
        var sortertePerioder = perioderUtenFriOpphold.stream()
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());

        while (!sortertePerioder.isEmpty() && sortertePerioder.get(sortertePerioder.size() - 1).isOpphold()) {
            sortertePerioder.remove(sortertePerioder.size() - 1);
        }
        if (sortertePerioder.isEmpty()) {
            return perioderUtenFriOpphold;
        }
        return sortertePerioder;
    }

    private LocalDate utledEndringsdatoVedRevurdering(UttakInput input) {
        if (input.getBehandlingReferanse().erRevurdering()) {
            return endringsdatoRevurderingUtleder.utledEndringsdato(input);
        }
        return null;
    }

    private List<OppgittPeriodeEntitet> justerFordelingEtterFamilieHendelse(ForeldrepengerGrunnlag fpGrunnlag,
                                                                            List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                            RelasjonsRolleType relasjonsRolleType,
                                                                            boolean ønskerJustertVedFødsel) {
        var gammelFamiliehendelse = gjeldenedFamiliehendelsedatoFraOrginalbehandling(fpGrunnlag)
            .orElseGet(() -> fpGrunnlag.getFamilieHendelser().getSøknadFamilieHendelse().getFamilieHendelseDato());
        var nyFamiliehendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        return JusterFordelingTjeneste.justerForFamiliehendelse(
            oppgittePerioder,
            gammelFamiliehendelse,
            nyFamiliehendelse,
            relasjonsRolleType,
            ønskerJustertVedFødsel
        );
    }

    private List<OppgittPeriodeEntitet> oppgittePerioderFraForrigeBehandling(Long forrigeBehandling) {
        var forrigeBehandlingYtelseFordeling = ytelsesFordelingRepository.hentAggregat(forrigeBehandling);
        return forrigeBehandlingYtelseFordeling.getOppgittFordeling().getPerioder();
    }

    private boolean behandlingHarUttaksresultat(Long forrigeBehandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(forrigeBehandlingId).isPresent();
    }

    private AvklarteUttakDatoerEntitet avklarteDatoerMedEndringsdato(Long behandlingId, LocalDate endringsdato) {
        var avklarteUttakDatoer = ytelsesFordelingRepository.hentAggregat(behandlingId).getAvklarteDatoer();
        var builder = new AvklarteUttakDatoerEntitet.Builder(avklarteUttakDatoer);
        return builder.medOpprinneligEndringsdato(endringsdato).medJustertEndringsdato(null).build();
    }

    private List<OppgittPeriodeEntitet> kopierVedtaksperioderFomEndringsdato(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                             LocalDate endringsdato,
                                                                             Long forrigeBehandling) {
        LOG.info("Kopierer vedtaksperioder fom endringsdato {} {}", endringsdato, forrigeBehandling);
        //Kopier vedtaksperioder fom endringsdato.
        var uttakResultatEntitet = fpUttakRepository.hentUttakResultat(forrigeBehandling);
        return VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, oppgittePerioder, endringsdato, false);
    }

    private static Optional<LocalDate> gjeldenedFamiliehendelsedatoFraOrginalbehandling(ForeldrepengerGrunnlag fpGrunnlag) {
        return fpGrunnlag.getOriginalBehandling().map(b -> b.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato());
    }

    private static boolean nySøknadPåTermin(FamilieHendelser familiehendels) {
        return familiehendels.getOverstyrtFamilieHendelse().isEmpty()
            && familiehendels.getSøknadFamilieHendelse().getFødselsdato().isEmpty()
            && familiehendels.getBekreftetFamilieHendelse().filter(fh -> fh.getFødselsdato().isPresent()).isEmpty();
    }

    private List<OppgittPeriodeEntitet> kopier(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().map(p -> OppgittPeriodeBuilder.fraEksisterende(p).build()).toList();
    }

    private List<OppgittPeriodeEntitet> getSøknadsPerioderOppdatertMedMottattDato(UttakInput input, YtelseFordelingAggregat aggregat) {
        var periodegrense = uttaksperiodegrenseRepository.hentHvisEksisterer(input.getBehandlingReferanse().behandlingId());
        if (periodegrense.isPresent()) {
            var mottattDato = periodegrense.orElseThrow().getMottattDato();
            return aggregat.getOppgittFordeling().getPerioder().stream()
                .map(p -> OppgittPeriodeBuilder.fraEksisterende(p)
                    .medTidligstMottattDato(utledMottattDato(p.getTidligstMottattDato().orElseGet(p::getMottattDato), mottattDato))
                    .build())
                .toList();
        } else {
            return aggregat.getOppgittFordeling().getPerioder();
        }
    }

    private static LocalDate utledMottattDato(LocalDate datoFraPeriode, LocalDate mottattdato) {
        return Optional.ofNullable(datoFraPeriode)
            .filter(d -> d.isBefore(mottattdato))
            .orElse(mottattdato);
    }
}
