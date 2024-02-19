package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
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
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;

@Dependent
public class FastsettUttaksgrunnlagTjeneste {

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
                justertePerioder = kopierVedtaksperioderFomEndringsdato(justertePerioder, endringsdatoRevurdering, originalBehandlingId,
                    input.getBehandlingReferanse().getSkjæringstidspunkt().kreverSammenhengendeUttak() || input.isBehandlingManueltOpprettet());
            } else {
                justertePerioder = oppgittePerioderFraForrigeBehandling(originalBehandlingId);
            }
        }

        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        if (fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            justertePerioder = justerFordelingEtterFamilieHendelse(fpGrunnlag, justertePerioder, ref.relasjonRolle(),
                fordeling.ønskerJustertVedFødsel());
        }
        if (ref.getSkjæringstidspunkt().kreverSammenhengendeUttak()) {
            justertePerioder = fjernOppholdsperioderLiggendeTilSlutt(justertePerioder);
        } else {
            justertePerioder = fjernOppholdsperioder(justertePerioder);
        }
        justertePerioder = leggTilUtsettelserForPleiepenger(input, justertePerioder);
        return new OppgittFordelingEntitet(kopier(justertePerioder), fordeling.getErAnnenForelderInformert(), fordeling.ønskerJustertVedFødsel());
    }

    private List<OppgittPeriodeEntitet> fjernOppholdsperioder(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream().filter(p -> !p.isOpphold()).toList();
    }

    private List<OppgittPeriodeEntitet> leggTilUtsettelserForPleiepenger(UttakInput input, List<OppgittPeriodeEntitet> perioder) {
        return PleiepengerJustering.juster(input.getBehandlingReferanse().aktørId(), input.getIayGrunnlag(), perioder);
    }

    private List<OppgittPeriodeEntitet> fjernOppholdsperioderLiggendeTilSlutt(List<OppgittPeriodeEntitet> perioder) {
        var sortertePerioder = perioder.stream()
                .sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom))
                .collect(Collectors.toList());

        while (!sortertePerioder.isEmpty() && sortertePerioder.get(sortertePerioder.size() - 1).isOpphold()) {
            sortertePerioder.remove(sortertePerioder.size() - 1);
        }
        if (sortertePerioder.isEmpty()) {
            return perioder;
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
        if (oppgittePerioder.isEmpty()) {
            throw new IllegalStateException("Skal ikke fødselsjustere når gjeldende behandling ikke har uttak (f.eks. ved opphør)");
        }
        var familiehendelser = finnFamiliehendelser(fpGrunnlag);
        return JusterFordelingTjeneste.justerForFamiliehendelse(oppgittePerioder, familiehendelser.søknad().orElse(null),
                familiehendelser.gjeldende(), relasjonsRolleType, ønskerJustertVedFødsel);
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
                                                                             Long forrigeBehandling,
                                                                             boolean kreverSammenhengendeUttak) {
        //Kopier vedtaksperioder fom endringsdato.
        var uttakResultatEntitet = fpUttakRepository.hentUttakResultat(forrigeBehandling);
        return VedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, oppgittePerioder, endringsdato, kreverSammenhengendeUttak);
    }

    private record FHSøknadGjeldende(Optional<LocalDate> søknad, LocalDate gjeldende) {
    }

    private FHSøknadGjeldende finnFamiliehendelser(ForeldrepengerGrunnlag fpGrunnlag) {
        var gjeldendeFødselsdato = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        var originalbehandling = fpGrunnlag.getOriginalBehandling();
        if (originalbehandling.isPresent()) {
            var fødselsdatoForrigeBehandling = originalbehandling.get()
                    .getFamilieHendelser()
                    .getGjeldendeFamilieHendelse()
                    .getFamilieHendelseDato();
            return new FHSøknadGjeldende(Optional.ofNullable(fødselsdatoForrigeBehandling), gjeldendeFødselsdato);
        }
        var søknadVersjon = fpGrunnlag.getFamilieHendelser().getSøknadFamilieHendelse();
        var søknadFødselsdato = søknadVersjon.getFødselsdato();
        var søknadTermindato = søknadVersjon.getTermindato();
        if (søknadTermindato.isPresent()) {
            if (søknadFødselsdato.isPresent()) {
                return new FHSøknadGjeldende(søknadFødselsdato, gjeldendeFødselsdato);
            }
            var termindato = søknadTermindato.get();
            return new FHSøknadGjeldende(Optional.of(termindato), gjeldendeFødselsdato);
        }
        return new FHSøknadGjeldende(søknadFødselsdato, gjeldendeFødselsdato);
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
