package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.søknadsfrist.SøktPeriodeTjeneste;
import no.nav.foreldrepenger.regler.SøknadsfristUtil;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakPeriodegrenseDto;

@ApplicationScoped
public class UttakPeriodegrenseDtoTjeneste {

    private SøknadRepository søknadRepository;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;

    @Inject
    public UttakPeriodegrenseDtoTjeneste(SøknadRepository søknadRepository,
                                         UttaksperiodegrenseRepository uttaksperiodegrenseRepository) {
        this.søknadRepository = søknadRepository;
        this.uttaksperiodegrenseRepository = uttaksperiodegrenseRepository;
    }

    UttakPeriodegrenseDtoTjeneste() {
        // For CDI
    }

    public Optional<UttakPeriodegrenseDto> mapFra(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        return uttaksperiodegrenseRepository.hentHvisEksisterer(ref.behandlingId())
            .map(g -> lagDto(input, g));
    }

    private UttakPeriodegrenseDto lagDto(UttakInput input, Uttaksperiodegrense uttaksperiodegrense) {
        var ref = input.getBehandlingReferanse();
        var dto = new UttakPeriodegrenseDto();
        dto.setSoknadsfristForForsteUttaksdato(uttaksperiodegrense.getFørsteLovligeUttaksdag());
        dto.setMottattDato(uttaksperiodegrense.getMottattDato());
        var søktPeriodeOpt = FagsakYtelseTypeRef.Lookup.find(SøktPeriodeTjeneste.class, ref.fagsakYtelseType())
            .orElseThrow()
            .finnSøktPeriode(input);

        søktPeriodeOpt.ifPresent(søktPeriode -> {
            var søknadsfrist = finnSøknadsfristForPeriodeMedStart(søktPeriode.getFomDato());
            dto.setSoknadsperiodeStart(søktPeriode.getFomDato());
            dto.setSoknadsperiodeSlutt(søktPeriode.getTomDato());
            dto.setSoknadsfristForForsteUttaksdato(søknadsfrist);
            søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId())
                .ifPresent(søknad -> dto.setAntallDagerLevertForSent(ChronoUnit.DAYS.between(søknadsfrist, søknad.getMottattDato())));
        });
        return dto;
    }

    LocalDate finnSøknadsfristForPeriodeMedStart(LocalDate periodeStart) {
        //TODO FP kode som brukes av SVP
        return SøknadsfristUtil.finnSøknadsfrist(periodeStart);
    }

}
