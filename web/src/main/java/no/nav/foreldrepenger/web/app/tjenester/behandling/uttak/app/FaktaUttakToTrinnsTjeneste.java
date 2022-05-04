package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.BekreftetOppgittPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.FaktaUttakDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SlettetUttakPeriodeDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.UttakDokumentasjonDto;

public class FaktaUttakToTrinnsTjeneste {

    /**
     * sett totrinns ved avklar fakta endring
     */
    public static boolean oppdaterTotrinnskontrollVedEndringerFaktaUttak(FaktaUttakDto dto) {
        return erDetEndringer(dto.getSlettedePerioder(), dto.getBekreftedePerioder());
    }

    private static boolean erDetEndringer(List<SlettetUttakPeriodeDto> slettedePerioder, List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        return (slettedePerioder != null && !slettedePerioder.isEmpty()) || harEndringerPåPerioder(bekreftedePerioder);
    }

    private static boolean harEndringerPåPerioder(List<BekreftetOppgittPeriodeDto> bekreftedePerioder) {
        var alleEndredePerioder = bekreftedePerioder
            .stream().filter(p -> isNotBlank(p.getBekreftetPeriode().getBegrunnelse()))
            .collect(Collectors.toList());

        List<Årsak> årsakerInnleggelse = Arrays.asList(UtsettelseÅrsak.INSTITUSJON_SØKER, UtsettelseÅrsak.INSTITUSJON_BARN);
        var innleggelsePerioder = alleEndredePerioder.stream()
            .filter(p -> p.getBekreftetPeriode().getUtsettelseÅrsak() != null && årsakerInnleggelse.contains(p.getBekreftetPeriode().getUtsettelseÅrsak()))
            .collect(Collectors.toList());

        if (alleEndredePerioder.size() != innleggelsePerioder.size()) {
            return true;
        }

        for (var periode : innleggelsePerioder) {
            var dokumentertePerioder = periode.getBekreftetPeriode().getDokumentertePerioder();
            if (dokumentertePerioder.isEmpty() || erNyPeriode(periode)) {
                return true;
            }

            if (erHelePeriodenDokumentert(periode, dokumentertePerioder)) {
                return false;
            }
        }
        return true;
    }

    private static boolean erHelePeriodenDokumentert(BekreftetOppgittPeriodeDto periode, List<UttakDokumentasjonDto> dokumentertePerioder) {
        var sortertDokPerioder = dokumentertePerioder.stream()
            .sorted(Comparator.comparing(UttakDokumentasjonDto::getFom))
            .collect(Collectors.toList());

        var startDato = periode.getBekreftetPeriode().getFom();
        for (var dokumentasjon : sortertDokPerioder) {
            if (!dokumentasjon.getFom().isAfter(startDato)) {
                startDato = dokumentasjon.getTom();
            }
            if (!startDato.isBefore(periode.getBekreftetPeriode().getTom())) {
                return true;
            }
        }
        return false;
    }

    private static boolean erNyPeriode(BekreftetOppgittPeriodeDto bkftUttakPeriodeDto) {
        return bkftUttakPeriodeDto.getOrginalFom() == null && bkftUttakPeriodeDto.getOrginalTom() == null;
    }

}
