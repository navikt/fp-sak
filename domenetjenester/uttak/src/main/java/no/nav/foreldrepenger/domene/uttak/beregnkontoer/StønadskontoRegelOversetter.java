package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
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

    public BeregnKontoerGrunnlag tilRegelmodell(YtelseFordelingAggregat ytelseFordelingAggregat,
                                                Dekningsgrad dekningsgrad,
                                                Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                ForeldrepengerGrunnlag fpGrunnlag,
                                                BehandlingReferanse ref) {

        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var annenForeldreHarRett = ytelseFordelingAggregat.harAnnenForelderRett(annenpartsGjeldendeUttaksplan.filter(ForeldrepengerUttak::harUtbetaling).isPresent());

        var grunnlagBuilder = BeregnKontoerGrunnlag.builder()
            .antallBarn(familieHendelse.getAntallBarn())
            .dekningsgrad(map(dekningsgrad))
            .brukerRolle(UttakEnumMapper.mapTilBeregning(ref.relasjonRolle()))
            .rettighetType(mapRettighet(ytelseFordelingAggregat, annenForeldreHarRett))
            .tidligereUtregning(mapTilStønadskontoBeregning(fpGrunnlag.getStønadskontoberegning()))
            .morHarUføretrygd(ytelseFordelingAggregat.morMottarUføretrygd(fpGrunnlag.getUføretrygdGrunnlag().orElse(null)))
            .familieHendelseDatoNesteSak(familieHendelseNesteSak(fpGrunnlag).orElse(null));


        leggTilFamileHendelseDatoer(grunnlagBuilder, familieHendelse, fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel());

        return grunnlagBuilder
            .build();
    }

    private void leggTilFamileHendelseDatoer(BeregnKontoerGrunnlag.Builder grunnlagBuilder,
                                             FamilieHendelse gjeldendeFamilieHendelse,
                                             boolean erFødsel) {
        if (erFødsel) {
            grunnlagBuilder.fødselsdato(gjeldendeFamilieHendelse.getFødselsdato().orElse(null));
            grunnlagBuilder.termindato(gjeldendeFamilieHendelse.getTermindato().orElse(null));
        } else {
            grunnlagBuilder.omsorgsovertakelseDato(gjeldendeFamilieHendelse.getOmsorgsovertakelse().orElseThrow());
        }
    }

    private static Rettighetstype mapRettighet(YtelseFordelingAggregat ytelseFordelingAggregat, boolean annenForeldreHarRett) {
        if (ytelseFordelingAggregat.harAleneomsorg()) {
            return Rettighetstype.ALENEOMSORG;
        } else if (annenForeldreHarRett) {
            return Rettighetstype.BEGGE_RETT;
        } else {
            return Rettighetstype.BARE_SØKER_RETT;
        }
    }

    private Optional<LocalDate> familieHendelseNesteSak(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        return foreldrepengerGrunnlag.getNesteSakGrunnlag().map(NesteSakGrunnlagEntitet::getHendelsedato);
    }

    private Map<StønadskontoKontotype, Integer> mapTilStønadskontoBeregning(Map<StønadskontoType, Integer> gjeldende) {
        return gjeldende.entrySet().stream()
            .collect(Collectors.toMap(e -> UttakEnumMapper.mapTilBeregning(e.getKey()), Map.Entry::getValue));
    }


}
