package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.UttakEnumMapper;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.stønadskonto.regelmodell.StønadskontoKontotype;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.BeregnKontoerGrunnlag;
import no.nav.foreldrepenger.stønadskonto.regelmodell.grunnlag.Rettighetstype;

public class StønadskontoRegelOversetter {

    private static final LocalDate START_FPSAK = LocalDate.of(2019, Month.JANUARY, 1);

    public static BeregnKontoerGrunnlag tilRegelmodell(YtelseFordelingAggregat ytelseFordelingAggregat,
                                                       LocalDate skjæringstidspunkt,
                                                       Dekningsgrad dekningsgrad,
                                                       Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                       ForeldrepengerGrunnlag fpGrunnlag,
                                                       BehandlingReferanse ref,
                                                       Map<StønadskontoType, Integer> tidligereUtregning,
                                                       UttakCore2024 uttakCore2024) {

        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var annenForeldreHarRett = ytelseFordelingAggregat.harAnnenForelderRett(annenpartsGjeldendeUttaksplan.filter(ForeldrepengerUttak::harUtbetaling).isPresent());

        var grunnlagBuilder = BeregnKontoerGrunnlag.builder()
            .regelvalgsdato(uttakCore2024.utledRegelvalgsdato(familieHendelse))
            .antallBarn(familieHendelse.getAntallBarn())
            .dekningsgrad(map(dekningsgrad))
            .brukerRolle(UttakEnumMapper.mapTilBeregning(ref.relasjonRolle()))
            .rettighetType(mapRettighet(ref.relasjonRolle(), ytelseFordelingAggregat, annenForeldreHarRett))
            .tidligereUtregning(mapTilStønadskontoBeregning(tidligereUtregning))
            .morHarUføretrygd(ytelseFordelingAggregat.morMottarUføretrygd(fpGrunnlag.getUføretrygdGrunnlag().orElse(null)))
            .familieHendelseDatoNesteSak(familieHendelseNesteSak(fpGrunnlag).orElse(null));


        leggTilFamileHendelseDatoer(grunnlagBuilder, familieHendelse, fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel());
        utledRegelvalgsdato(skjæringstidspunkt, dekningsgrad, familieHendelse).ifPresent(grunnlagBuilder::regelvalgsdato);
        return grunnlagBuilder
            .build();
    }

    private static void leggTilFamileHendelseDatoer(BeregnKontoerGrunnlag.Builder grunnlagBuilder,
                                             FamilieHendelse gjeldendeFamilieHendelse,
                                             boolean erFødsel) {
        if (erFødsel) {
            grunnlagBuilder.fødselsdato(gjeldendeFamilieHendelse.getFødselsdato().orElse(null));
            grunnlagBuilder.termindato(gjeldendeFamilieHendelse.getTermindato().orElse(null));
        } else {
            grunnlagBuilder.omsorgsovertakelseDato(gjeldendeFamilieHendelse.getOmsorgsovertakelse().orElseThrow());
        }
    }

    private static Rettighetstype mapRettighet(RelasjonsRolleType relasjonsRolleType, YtelseFordelingAggregat ytelseFordelingAggregat, boolean annenForeldreHarRett) {
        // Skal være avklart på dette tidspunktet - forsiktig med metoder som har fallback til oppgitt rettighet pga nyere førstegangssøknader.
        if (ytelseFordelingAggregat.robustHarAleneomsorg(relasjonsRolleType)) {
            return Rettighetstype.ALENEOMSORG;
        } else if (annenForeldreHarRett) {
            return Rettighetstype.BEGGE_RETT;
        } else {
            return Rettighetstype.BARE_SØKER_RETT;
        }
    }

    private static Optional<LocalDate> familieHendelseNesteSak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getHendelsedato);
    }

    private static Map<StønadskontoKontotype, Integer> mapTilStønadskontoBeregning(Map<StønadskontoType, Integer> gjeldende) {
        return gjeldende.entrySet().stream()
            .collect(Collectors.toMap(e -> UttakEnumMapper.mapTilBeregning(e.getKey()), Map.Entry::getValue));
    }

    /*
     * Normalt er ikrafttredelse lagt til fødsel eller omsorgsovertakelse etter en gitt dato. Unntak:
     * - LOV-2018-12-07-90 om tredeling av 80% uttak - gjelder for tilfelle der uttaket begynner etter 1/1-2019
     *
     * Ikrafttredelsesmekanismer kan også foreligge her
     */
    private static Optional<LocalDate> utledRegelvalgsdato(LocalDate skjæringstidspunkt, Dekningsgrad dekningsgrad, FamilieHendelse familieHendelse) {
        if (familieHendelse.getFamilieHendelseDato().isBefore(START_FPSAK) && dekningsgrad.isÅtti()) {
            return Optional.ofNullable(skjæringstidspunkt).filter(stp -> !stp.isBefore(START_FPSAK));
        }
        return Optional.empty();
    }




}
