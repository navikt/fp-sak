package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.behandling.steg.søknadsfrist.fp.SøknadsfristTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakPeriodegrenseDto;

@ApplicationScoped
public class UttakPeriodegrenseDtoTjeneste {

    private SøknadRepository søknadRepository;
    private SøknadsfristTjeneste vurderSøknadsfristTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public UttakPeriodegrenseDtoTjeneste() {
        // For CDI
    }

    @Inject
    public UttakPeriodegrenseDtoTjeneste(SøknadRepository søknadRepository,
                                         BehandlingsresultatRepository behandlingsresultatRepository,
                                         SøknadsfristTjeneste søknadsfristTjeneste) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.vurderSøknadsfristTjeneste = søknadsfristTjeneste;
        this.søknadRepository = søknadRepository;
    }

    public Optional<UttakPeriodegrenseDto> mapFra(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        Optional<Behandlingsresultat> behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (behandlingsresultatOpt.isPresent()) {
            Optional<Uttaksperiodegrense> gjeldendeUttaksperiodegrense = behandlingsresultatOpt.get().getGjeldendeUttaksperiodegrense();
            if (gjeldendeUttaksperiodegrense.isPresent()) {
                Uttaksperiodegrense grense = gjeldendeUttaksperiodegrense.get();
                UttakPeriodegrenseDto dto = new UttakPeriodegrenseDto();
                dto.setSoknadsfristForForsteUttaksdato(grense.getFørsteLovligeUttaksdag());
                dto.setMottattDato(grense.getMottattDato());

                populerDto(dto, input);

                return Optional.of(dto);
            }
        }
        return Optional.empty();
    }

    private void populerDto(UttakPeriodegrenseDto dto, UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var søktPeriodeOpt = FagsakYtelseTypeRef.Lookup.find(SøktPeriodeTjeneste.class, ref.getFagsakYtelseType()).orElseThrow().finnSøktPeriode(input);

        søktPeriodeOpt.ifPresent(søktPeriode -> {
            var søknadsfrist = vurderSøknadsfristTjeneste.finnSøknadsfristForPeriodeMedStart(søktPeriode.getFomDato());
            var søknad = søknadRepository.hentSøknad(ref.getBehandlingId());
            dto.setSoknadsperiodeStart(søktPeriode.getFomDato());
            dto.setSoknadsperiodeSlutt(søktPeriode.getTomDato());
            dto.setSoknadsfristForForsteUttaksdato(søknadsfrist);
            if (søknad != null) {
                dto.setAntallDagerLevertForSent(ChronoUnit.DAYS.between(søknadsfrist, søknad.getMottattDato()));
            }
        });
    }

}
