package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.grunnlag.BeregnKontoerGrunnlag;

public class StønadskontoRegelOversetter {

    public BeregnKontoerGrunnlag tilRegelmodell(RelasjonsRolleType relasjonsRolleType,
                                                YtelseFordelingAggregat ytelseFordelingAggregat,
                                                boolean harSøkerRett,
                                                FagsakRelasjon fagsakRelasjon,
                                                Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                ForeldrepengerGrunnlag fpGrunnlag) {

        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var annenForeldreHarRett = UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat,
            annenpartsGjeldendeUttaksplan);

        var grunnlagBuilder = BeregnKontoerGrunnlag.builder()
            .medAntallBarn(familieHendelse.getAntallBarn())
            .medDekningsgrad(map(fagsakRelasjon.getGjeldendeDekningsgrad().getVerdi()));

        leggTilFamileHendelseDatoer(grunnlagBuilder, familieHendelse,
            fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel());

        var aleneomsorg = UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat);
        if (relasjonsRolleType.equals(RelasjonsRolleType.MORA)) {
            return grunnlagBuilder.morRett(harSøkerRett)
                .farRett(annenForeldreHarRett)
                .morAleneomsorg(aleneomsorg)
                .build();
        }
        return grunnlagBuilder.morRett(annenForeldreHarRett).farRett(harSøkerRett).farAleneomsorg(aleneomsorg).build();
    }

    private void leggTilFamileHendelseDatoer(BeregnKontoerGrunnlag.Builder grunnlagBuilder,
                                             FamilieHendelse gjeldendeFamilieHendelse,
                                             boolean erFødsel) {
        if (erFødsel) {
            grunnlagBuilder.medFødselsdato(gjeldendeFamilieHendelse.getFødselsdato().orElse(null));
            grunnlagBuilder.medTermindato(gjeldendeFamilieHendelse.getTermindato().orElse(null));
        } else {
            grunnlagBuilder.medOmsorgsovertakelseDato(gjeldendeFamilieHendelse.getOmsorgsovertakelse().orElseThrow());
        }
    }
}
