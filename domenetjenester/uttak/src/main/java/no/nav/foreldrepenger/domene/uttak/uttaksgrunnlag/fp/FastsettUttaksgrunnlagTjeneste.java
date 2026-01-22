package no.nav.foreldrepenger.domene.uttak.uttaksgrunnlag.fp;

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

        if (gjelderTerminFødsel(input)) {
            justertePerioder = justerFordelingEtterFamilieHendelse(input.getYtelsespesifiktGrunnlag(), justertePerioder, ref.relasjonRolle(), fordeling.ønskerJustertVedFødsel());
        }
        justertePerioder = slåSammenLikePerioder(justertePerioder);
        justertePerioder = fjernOppholdsperioderTilSluttForSammenhengendeUttak(justertePerioder);
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

    private static boolean gjelderTerminFødsel(UttakInput input) {
        var fpGrunnlag = (ForeldrepengerGrunnlag) input.getYtelsespesifiktGrunnlag();
        return fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel();
    }

    private List<OppgittPeriodeEntitet> leggTilUtsettelserForPleiepenger(UttakInput input, List<OppgittPeriodeEntitet> perioder) {
        return PleiepengerJustering.juster(input.getBehandlingReferanse().aktørId(), input.getIayGrunnlag(), perioder);
    }

    private List<OppgittPeriodeEntitet> fjernOppholdsperioderTilSluttForSammenhengendeUttak(List<OppgittPeriodeEntitet> perioder) {
        var sortertePerioder = perioder.stream().sorted(Comparator.comparing(OppgittPeriodeEntitet::getFom)).collect(Collectors.toList());

        while (!sortertePerioder.isEmpty() && UtsettelseCore2021.kreverSammenhengendeUttak(sortertePerioder.getLast()) && sortertePerioder.getLast()
            .isOpphold()) {
            sortertePerioder.removeLast();
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
        var gammelFamiliehendelse = utledFamiliehendelsesdatoSomUttaksplanenTarUtgangspunktI(fpGrunnlag);
        var nyFamiliehendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse().getFamilieHendelseDato();
        return JusterFordelingTjeneste.justerForFamiliehendelse(
            oppgittePerioder,
            gammelFamiliehendelse,
            nyFamiliehendelse,
            relasjonsRolleType,
            ønskerJustertVedFødsel
        );
    }

    private static LocalDate utledFamiliehendelsesdatoSomUttaksplanenTarUtgangspunktI(ForeldrepengerGrunnlag fpGrunnlag) {
        var familiehendelsedatoSøkerTokUtgangspunktIGjeldendeBehandling = fpGrunnlag.getFamilieHendelser().getSøknadFamilieHendelse().getFamilieHendelseDato();
        var originalBehandlingOpt = fpGrunnlag.getOriginalBehandling();
        if (originalBehandlingOpt.isEmpty()) {
            return familiehendelsedatoSøkerTokUtgangspunktIGjeldendeBehandling;
        }

        var originalFamiliehendelser = originalBehandlingOpt.get().getFamilieHendelser();
        var familiehendelsedatoSøkerTokUtgangspunktIOriginalBehandling = originalFamiliehendelser.getSøknadFamilieHendelse().getFamilieHendelseDato();
        if (!familiehendelsedatoSøkerTokUtgangspunktIGjeldendeBehandling.isEqual(familiehendelsedatoSøkerTokUtgangspunktIOriginalBehandling)) {
            // Hvis familiehendelsesdatoen for søknaden i gjeldende behandling er forskjellig fra den i original behandling,
            // skal vi bruke datoen fra gjeldende behandling. Dette gjelder typisk i tilfeller hvor en ny førstegangssøknad
            // har endret familiehendelsesgrunnlaget.
            //
            // Eksempler:
            // - Søker sender inn ny førstegangssøknad med oppdatert termindato.
            // - Søker sender inn ny førstegangssøknad som baserer på fødselsdato i stedet for tidligere termindato.
            return familiehendelsedatoSøkerTokUtgangspunktIGjeldendeBehandling;
        }

        return originalFamiliehendelser.getGjeldendeFamilieHendelse().getFamilieHendelseDato();
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
