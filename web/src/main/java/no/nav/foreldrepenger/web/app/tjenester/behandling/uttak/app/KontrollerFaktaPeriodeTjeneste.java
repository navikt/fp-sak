package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.AvklarFaktaUttakPerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaData;
import no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaDataDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeDto;

@ApplicationScoped
public class KontrollerFaktaPeriodeTjeneste {

    private AvklarFaktaUttakPerioderTjeneste tjeneste;
    private UttakInputTjeneste uttakInputTjeneste;


    public KontrollerFaktaPeriodeTjeneste() {
        //for CDI proxy
    }

    @Inject
    public KontrollerFaktaPeriodeTjeneste(AvklarFaktaUttakPerioderTjeneste tjeneste,
                                          UttakInputTjeneste uttakInputTjeneste) {
        this.tjeneste = tjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    public KontrollerFaktaDataDto hentKontrollerFaktaPerioder(Long behandlingId) {
        var input = uttakInputTjeneste.lagInput(behandlingId);
        if (input.isSkalBrukeNyFaktaOmUttak()) {
            return new KontrollerFaktaDataDto(List.of());
        }
        return mapTilDto(tjeneste.hentKontrollerFaktaPerioder(input));
    }

    private KontrollerFaktaDataDto mapTilDto(KontrollerFaktaData kontrollerFaktaData) {
        var dtoPerioder = kontrollerFaktaData.getPerioder().stream()
            .map(periode -> new KontrollerFaktaPeriodeDto.Builder(periode, arbeidsgiverReferanse(periode)).build()).collect(Collectors.toList());
        return new KontrollerFaktaDataDto(dtoPerioder);
    }

    private String arbeidsgiverReferanse(KontrollerFaktaPeriode periode) {
        var arbeidsgiver = periode.getOppgittPeriode().getArbeidsgiver();
        return arbeidsgiver == null ?  null : arbeidsgiver.getIdentifikator();
    }
}
