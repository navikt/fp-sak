package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.EndringsdatoRevurderingUtleder;

@Dependent
public class FastsettUttaksgrunnlagTjeneste {

    private final FpUttakRepository fpUttakRepository;
    private final YtelsesFordelingRepository ytelsesFordelingRepository;
    private final EndringsdatoFørstegangsbehandlingUtleder endringsdatoFørstegangsbehandlingUtleder;
    private final EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder;

    private final VedtaksperioderHelper vedtaksperioderHelper = new VedtaksperioderHelper();
    private final JusterFordelingTjeneste justerFordelingTjeneste = new JusterFordelingTjeneste();

    @Inject
    public FastsettUttaksgrunnlagTjeneste(UttakRepositoryProvider provider,
                                          EndringsdatoFørstegangsbehandlingUtleder endringsdatoFørstegangsbehandlingUtleder,
                                          @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) EndringsdatoRevurderingUtleder endringsdatoRevurderingUtleder) {
        this.fpUttakRepository = provider.getFpUttakRepository();
        this.ytelsesFordelingRepository = provider.getYtelsesFordelingRepository();
        this.endringsdatoFørstegangsbehandlingUtleder = endringsdatoFørstegangsbehandlingUtleder;
        this.endringsdatoRevurderingUtleder = endringsdatoRevurderingUtleder;
    }

    public void fastsettUttaksgrunnlag(UttakInput input) {
        var endringsdatoRevurdering = utledEndringsdatoVedRevurdering(input);
        var justertFordeling = justerFordeling(input, endringsdatoRevurdering);
        var behandlingId = input.getBehandlingReferanse().behandlingId();
        //Endringsdato skal utledes før justering ved revurdering, men etter justering for førstegangsbehandlinger
        LocalDate endringsdato;
        if (input.getBehandlingReferanse().erRevurdering()) {
            endringsdato = endringsdatoRevurdering;
        } else {
            endringsdato = endringsdatoFørstegangsbehandlingUtleder.utledEndringsdato(input.getBehandlingReferanse().behandlingId(),
                    justertFordeling.getOppgittePerioder());
        }
        var avklarteUttakDatoer = avklarteDatoerMedEndringsdato(behandlingId, endringsdato);
        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
                .medJustertFordeling(justertFordeling)
                .medAvklarteDatoer(avklarteUttakDatoer);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private OppgittFordelingEntitet justerFordeling(UttakInput input, LocalDate endringsdatoRevurdering) {
        var ref = input.getBehandlingReferanse();
        var behandlingId = ref.behandlingId();
        var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var fordeling = ytelseFordelingAggregat.getOppgittFordeling();
        var justertePerioder = ytelseFordelingAggregat.getOppgittFordeling().getOppgittePerioder();
        if (ref.erRevurdering()) {
            var originalBehandlingId = ref.getOriginalBehandlingId();
            if (originalBehandlingId.isEmpty()) {
                throw new IllegalArgumentException("Utvikler-feil: ved revurdering skal det alltid finnes en original behandling");
            }
            if (behandlingHarUttaksresultat(originalBehandlingId.get())) {
                justertePerioder = kopierVedtaksperioderFomEndringsdato(justertePerioder, endringsdatoRevurdering,
                        originalBehandlingId.get());
            } else {
                justertePerioder = oppgittePerioderFraForrigeBehandling(originalBehandlingId.get());
            }
        }

        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();
        if (fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel()) {
            justertePerioder = justerFordelingEtterFamilieHendelse(fpGrunnlag, justertePerioder);
        }
        if (ref.getSkjæringstidspunkt().kreverSammenhengendeUttak()) {
            justertePerioder = fjernOppholdsperioderLiggendeTilSlutt(justertePerioder);
        } else {
            justertePerioder = fjernOppholdsperioder(justertePerioder);
        }
        justertePerioder = leggTilUtsettelserForPleiepenger(input, justertePerioder);
        return new OppgittFordelingEntitet(kopier(justertePerioder), fordeling.getErAnnenForelderInformert());
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
                                                                            List<OppgittPeriodeEntitet> oppgittePerioder) {
        var familiehendelser = finnFamiliehendelser(fpGrunnlag);
        return justerFordelingTjeneste.justerForFamiliehendelse(oppgittePerioder, familiehendelser.søknad().orElse(null),
                familiehendelser.gjeldende());
    }

    private List<OppgittPeriodeEntitet> oppgittePerioderFraForrigeBehandling(Long forrigeBehandling) {
        var forrigeBehandlingYtelseFordeling = ytelsesFordelingRepository.hentAggregat(forrigeBehandling);
        return forrigeBehandlingYtelseFordeling.getOppgittFordeling().getOppgittePerioder();
    }

    private boolean behandlingHarUttaksresultat(Long forrigeBehandlingId) {
        return fpUttakRepository.hentUttakResultatHvisEksisterer(forrigeBehandlingId).isPresent();
    }

    private AvklarteUttakDatoerEntitet avklarteDatoerMedEndringsdato(Long behandlingId, LocalDate endringsdato) {
        var avklarteUttakDatoer = ytelsesFordelingRepository.hentAggregat(behandlingId).getAvklarteDatoer();
        var builder = new AvklarteUttakDatoerEntitet.Builder(avklarteUttakDatoer);
        return builder.medOpprinneligEndringsdato(endringsdato).build();
    }

    private List<OppgittPeriodeEntitet> kopierVedtaksperioderFomEndringsdato(List<OppgittPeriodeEntitet> oppgittePerioder,
                                                                             LocalDate endringsdato,
                                                                             Long forrigeBehandling) {
        //Kopier vedtaksperioder fom endringsdato.
        var uttakResultatEntitet = fpUttakRepository.hentUttakResultat(forrigeBehandling);
        return vedtaksperioderHelper.opprettOppgittePerioder(uttakResultatEntitet, oppgittePerioder, endringsdato);
    }

    private static record FHSøknadGjeldende(Optional<LocalDate> søknad, LocalDate gjeldende) {
    }

    private FHSøknadGjeldende finnFamiliehendelser(ForeldrepengerGrunnlag fpGrunnlag) {
        var gjeldendeFødselsdato = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        if (fpGrunnlag.getOriginalBehandling().isPresent()) {
            var fødselsdatoForrigeBehandling = fpGrunnlag.getOriginalBehandling()
                    .get()
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
        return perioder.stream().map(p -> OppgittPeriodeBuilder.fraEksisterende(p).build()).collect(Collectors.toList());
    }
}
