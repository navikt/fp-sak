package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.AvklarAnnenforelderHarRettDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SlettetUttakPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakDokumentasjonDto;

@ApplicationScoped
public class FaktaUttakToTrinnsTjeneste {

    private YtelseFordelingTjeneste ytelseFordelingTjeneste;

    FaktaUttakToTrinnsTjeneste() {
        //For CDI proxy
    }

    @Inject
    public FaktaUttakToTrinnsTjeneste(YtelseFordelingTjeneste ytelseFordelingTjeneste) {
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
    }

    /**
     * sett totrinns ved avklar fakta endring
     */
    public boolean oppdaterTotrinnskontrollVedEndringerFaktaUttak(FaktaUttakDto dto) {
        return erDetEndringer(dto.getSlettedePerioder(), dto.getBekreftedePerioder());
    }

    private boolean erDetEndringer(List<SlettetUttakPeriodeDto> slettedePerioder, List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        return (slettedePerioder != null && !slettedePerioder.isEmpty()) || harEndringerPåPerioder(bekreftedePerioder);
    }

    private boolean harEndringerPåPerioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        List<BekreftetOppgittPeriodeDto> alleEndredePerioder = bekreftedePerioder
            .stream().filter(p -> isNotBlank(p.getBekreftetPeriode().getBegrunnelse()))
            .collect(Collectors.toList());

        List<Årsak> årsakerInnleggelse = Arrays.asList(UtsettelseÅrsak.INSTITUSJON_SØKER, UtsettelseÅrsak.INSTITUSJON_BARN);
        List<BekreftetOppgittPeriodeDto> innleggelsePerioder = alleEndredePerioder.stream()
            .filter(p -> p.getBekreftetPeriode().getUtsettelseÅrsak() != null && årsakerInnleggelse.contains(p.getBekreftetPeriode().getUtsettelseÅrsak()))
            .collect(Collectors.toList());

        if (alleEndredePerioder.size() != innleggelsePerioder.size()) {
            return true;
        }

        for (BekreftetOppgittPeriodeDto periode : innleggelsePerioder) {
            List<UttakDokumentasjonDto> dokumentertePerioder = periode.getBekreftetPeriode().getDokumentertePerioder();
            if (dokumentertePerioder.isEmpty() || erNyPeriode(periode)) {
                return true;
            }

            if (erHelePeriodenDokumentert(periode, dokumentertePerioder)) {
                return false;
            }
        }
        return true;
    }

    private boolean erHelePeriodenDokumentert(BekreftetOppgittPeriodeDto periode, List<UttakDokumentasjonDto> dokumentertePerioder) {
        List<UttakDokumentasjonDto> sortertDokPerioder = dokumentertePerioder.stream()
            .sorted(Comparator.comparing(UttakDokumentasjonDto::getFom))
            .collect(Collectors.toList());

        LocalDate startDato = periode.getBekreftetPeriode().getFom();
        for (UttakDokumentasjonDto dokumentasjon : sortertDokPerioder) {
            if (!dokumentasjon.getFom().isAfter(startDato)) {
                startDato = dokumentasjon.getTom();
            }
            if (!startDato.isBefore(periode.getBekreftetPeriode().getTom())) {
                return true;
            }
        }
        return false;
    }

    private boolean erNyPeriode(BekreftetOppgittPeriodeDto bkftUttakPeriodeDto) {
        return bkftUttakPeriodeDto.getOrginalFom() == null && bkftUttakPeriodeDto.getOrginalTom() == null;
    }

    /**
     * sett totrinns ved avklar annen forelder har ikke rett endring
     */
    public boolean oppdaterToTrinnskontrollVedEndringerAnnenforelderHarRett(AvklarAnnenforelderHarRettDto dto, Behandling behandling){

        YtelseFordelingAggregat ytelseFordelingAggregat = ytelseFordelingTjeneste.hentAggregat(behandling.getId());
        var rettAvkaring = ytelseFordelingAggregat.getAnnenForelderRettAvklaring();
        Boolean harAnnenForeldreRettSokVersjon = ytelseFordelingAggregat
            .getOppgittRettighet().getHarAnnenForeldreRett();

        Boolean harAnnenForeldreRettBekreftetVersjon = null;
        if (rettAvkaring.isPresent()) {
            harAnnenForeldreRettBekreftetVersjon = rettAvkaring.get();
        }

        boolean avkreftetBrukersOpplysinger = erEndretBekreftetVersjonEllerAvkreftetBrukersOpplysinger(harAnnenForeldreRettSokVersjon, dto.getAnnenforelderHarRett());
        boolean erEndretBekreftetVersjon = erEndretBekreftetVersjonEllerAvkreftetBrukersOpplysinger(harAnnenForeldreRettBekreftetVersjon, dto.getAnnenforelderHarRett());


        return setToTrinns(rettAvkaring, erEndretBekreftetVersjon, avkreftetBrukersOpplysinger);
    }

    private boolean erEndretBekreftetVersjonEllerAvkreftetBrukersOpplysinger(Object original, Object bekreftet) {
        return !Objects.equals(bekreftet, original);
    }

    private boolean setToTrinns(Optional<Boolean> rettAvklaring, boolean erEndretBekreftetVersjon, boolean avkreftetBrukersOpplysinger) {
        // Totrinns er sett hvis saksbehandler avkreftet først gang eller endret etter han bekreftet
        return avkreftetBrukersOpplysinger || (erEndretBekreftetVersjon && rettAvklaring.isPresent());
    }
}
