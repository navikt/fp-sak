package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.steg.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.SøknadsfristUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakPeriodegrenseDto;

@ApplicationScoped
public class UttakPeriodegrenseDtoTjeneste {

    private SøknadRepository søknadRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public UttakPeriodegrenseDtoTjeneste(SøknadRepository søknadRepository,
                                         BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.søknadRepository = søknadRepository;
    }

    UttakPeriodegrenseDtoTjeneste() {
        // For CDI
    }

    public Optional<UttakPeriodegrenseDto> mapFra(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(ref.getBehandlingId());
        if (behandlingsresultatOpt.isPresent()) {
            var gjeldendeUttaksperiodegrense = behandlingsresultatOpt.get().getGjeldendeUttaksperiodegrense();
            if (gjeldendeUttaksperiodegrense.isPresent()) {
                var grense = gjeldendeUttaksperiodegrense.get();
                var dto = new UttakPeriodegrenseDto();
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
        var søktPeriodeOpt = FagsakYtelseTypeRef.Lookup.find(SøktPeriodeTjeneste.class, ref.getFagsakYtelseType())
            .orElseThrow()
            .finnSøktPeriode(input);

        søktPeriodeOpt.ifPresent(søktPeriode -> {
            var søknadsfrist = finnSøknadsfristForPeriodeMedStart(søktPeriode.getFomDato());
            var søknad = søknadRepository.hentSøknad(ref.getBehandlingId());
            dto.setSoknadsperiodeStart(søktPeriode.getFomDato());
            dto.setSoknadsperiodeSlutt(søktPeriode.getTomDato());
            dto.setSoknadsfristForForsteUttaksdato(søknadsfrist);
            if (søknad != null) {
                dto.setAntallDagerLevertForSent(ChronoUnit.DAYS.between(søknadsfrist, søknad.getMottattDato()));
            }
        });
    }

    LocalDate finnSøknadsfristForPeriodeMedStart(LocalDate periodeStart) {
        //TODO FP kode som brukes av SVP
        return SøknadsfristUtil.finnSøknadsfrist(periodeStart);
    }

}
