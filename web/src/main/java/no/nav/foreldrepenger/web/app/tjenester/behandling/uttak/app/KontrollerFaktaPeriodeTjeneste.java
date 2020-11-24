package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.AvklarFaktaUttakPerioderTjeneste;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.KontrollerFaktaData;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder.KontrollerFaktaPeriode;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaDataDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.KontrollerFaktaPeriodeDto;

@ApplicationScoped
public class KontrollerFaktaPeriodeTjeneste {

    private AvklarFaktaUttakPerioderTjeneste tjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;


    public KontrollerFaktaPeriodeTjeneste() {
        //for CDI proxy
    }

    @Inject
    public KontrollerFaktaPeriodeTjeneste(AvklarFaktaUttakPerioderTjeneste tjeneste,
                                          ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                          UttakInputTjeneste uttakInputTjeneste) {
        this.tjeneste = tjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
    }

    public KontrollerFaktaDataDto hentKontrollerFaktaPerioder(Long behandlingId) {
        var input = uttakInputTjeneste.lagInput(behandlingId);
        return mapTilDto(tjeneste.hentKontrollerFaktaPerioder(input));
    }

    private KontrollerFaktaDataDto mapTilDto(KontrollerFaktaData kontrollerFaktaData) {
        List<KontrollerFaktaPeriodeDto> dtoPerioder = kontrollerFaktaData.getPerioder().stream()
            .map(periode -> new KontrollerFaktaPeriodeDto.Builder(periode, arbeidsgiverDto(periode), arbeidsgiverReferanse(periode)).build()).collect(Collectors.toList());
        return new KontrollerFaktaDataDto(dtoPerioder);
    }

    private String arbeidsgiverReferanse(KontrollerFaktaPeriode periode) {
        Arbeidsgiver arbeidsgiver = periode.getOppgittPeriode().getArbeidsgiver();
        return arbeidsgiver == null ?  null : arbeidsgiver.getIdentifikator();
    }

    private ArbeidsgiverDto arbeidsgiverDto(KontrollerFaktaPeriode periode) {
        Arbeidsgiver arbeidsgiver = periode.getOppgittPeriode().getArbeidsgiver();
        if (arbeidsgiver == null) {
            return null;
        }
        if (arbeidsgiver.getErVirksomhet()) {
            Virksomhet virksomhet = arbeidsgiverTjeneste.hentVirksomhet(arbeidsgiver.getOrgnr());
            return ArbeidsgiverDto.virksomhet(arbeidsgiver.getIdentifikator(), virksomhet.getNavn());
        }
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        return ArbeidsgiverDto.person(opplysninger.getNavn(), arbeidsgiver.getAktørId(), opplysninger.getFødselsdato());
    }
}
