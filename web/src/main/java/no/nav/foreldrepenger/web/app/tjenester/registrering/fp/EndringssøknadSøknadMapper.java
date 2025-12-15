package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;
import no.nav.vedtak.felles.xml.soeknad.endringssoeknad.v3.ObjectFactory;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Fordeling;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@ApplicationScoped
public class EndringssøknadSøknadMapper implements SøknadMapper {

    @Override
    public <V extends ManuellRegistreringDto> Soeknad mapSøknad(V registreringDto, NavBruker navBruker) {
        var søknad = SøknadMapperFelles.mapSøknad(registreringDto, navBruker);

        var endringssøknad = new ObjectFactory().createEndringssoeknad();
        endringssøknad.setFordeling(mapFordelingEndringssøknad((ManuellRegistreringEndringsøknadDto)registreringDto));
        var omYtelse = new no.nav.vedtak.felles.xml.soeknad.v3.ObjectFactory().createOmYtelse();
        omYtelse.getAny().add(new ObjectFactory().createEndringssoeknad(endringssøknad));
        søknad.setOmYtelse(omYtelse);
        return søknad;
    }

    private static Fordeling mapFordelingEndringssøknad(ManuellRegistreringEndringsøknadDto registreringDto) {
        var fordeling = new Fordeling();
        var perioder = mapFordelingPerioder(registreringDto.getTidsromPermisjon(), registreringDto.getSøker());
        fordeling.getPerioder().addAll(perioder.stream().filter(Objects::nonNull).toList());
        fordeling.setAnnenForelderErInformert(registreringDto.getAnnenForelderInformert());
        return fordeling;
    }

    private static List<LukketPeriodeMedVedlegg> mapFordelingPerioder(TidsromPermisjonDto tidsromPermisjon, ForeldreType soker) {
        List<LukketPeriodeMedVedlegg> result = new ArrayList<>();
        if (!isNull(tidsromPermisjon)) {
            result.addAll(mapOverføringsperioder(tidsromPermisjon.getOverføringsperioder(), soker));
            result.addAll(mapUtsettelsesperioder(tidsromPermisjon.getUtsettelsePeriode()));
            result.addAll(mapUttaksperioder(tidsromPermisjon.getPermisjonsPerioder()));
            result.addAll(mapGraderingsperioder(tidsromPermisjon.getGraderingPeriode()));
            result.addAll(mapOppholdsperioder(tidsromPermisjon.getOppholdPerioder()));
        }
        return result;
    }

    private static List<Uttaksperiode> mapUttaksperioder(List<PermisjonPeriodeDto> permisjonsPerioder) {
        return YtelseSøknadMapper.mapUttaksperioder(permisjonsPerioder);
    }

    private static List<Uttaksperiode> mapGraderingsperioder(List<GraderingDto> graderingsperioder) {
       return YtelseSøknadMapper.mapGraderingsperioder(graderingsperioder);
    }

    private static List<Oppholdsperiode> mapOppholdsperioder(List<OppholdDto> oppholdPerioder) {
       return YtelseSøknadMapper.mapOppholdsperioder(oppholdPerioder);
    }

    private static List<Utsettelsesperiode> mapUtsettelsesperioder(List<UtsettelseDto> utsettelserDto) {
        if (isNull(utsettelserDto)) {
            return new ArrayList<>();
        }
        return utsettelserDto.stream().map(EndringssøknadSøknadMapper::mapUtsettelsesperiode).toList();
    }

    private static Utsettelsesperiode mapUtsettelsesperiode(UtsettelseDto utsettelserDto) {
        return YtelseSøknadMapper.mapUtsettelsesperiode(utsettelserDto);
    }

    private static List<Overfoeringsperiode> mapOverføringsperioder(List<OverføringsperiodeDto> overføringsperioder, ForeldreType soker) {
        return YtelseSøknadMapper.mapOverføringsperioder(overføringsperioder, soker);
    }

}
