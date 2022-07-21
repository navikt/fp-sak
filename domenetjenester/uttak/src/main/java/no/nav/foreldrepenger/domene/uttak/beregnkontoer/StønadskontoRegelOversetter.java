package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static no.nav.foreldrepenger.domene.uttak.UttakEnumMapper.map;

import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.UttakOmsorgUtil;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.grunnlag.BeregnKontoerGrunnlag;

public class StønadskontoRegelOversetter {

    public BeregnKontoerGrunnlag tilRegelmodell(YtelseFordelingAggregat ytelseFordelingAggregat,
                                                FagsakRelasjon fagsakRelasjon,
                                                Optional<ForeldrepengerUttak> annenpartsGjeldendeUttaksplan,
                                                ForeldrepengerGrunnlag fpGrunnlag,
                                                BehandlingReferanse ref) {

        var familieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var annenForeldreHarRett = UttakOmsorgUtil.harAnnenForelderRett(ytelseFordelingAggregat, annenpartsGjeldendeUttaksplan);

        var grunnlagBuilder = BeregnKontoerGrunnlag.builder()
            .antallBarn(familieHendelse.getAntallBarn())
            .dekningsgrad(map(fagsakRelasjon.getGjeldendeDekningsgrad().getVerdi()));

        leggTilFamileHendelseDatoer(grunnlagBuilder, familieHendelse, fpGrunnlag.getFamilieHendelser().gjelderTerminFødsel());

        var aleneomsorg = UttakOmsorgUtil.harAleneomsorg(ytelseFordelingAggregat);
        if (ref.relasjonRolle().equals(RelasjonsRolleType.MORA)) {
            return grunnlagBuilder.morRett(true).farRett(annenForeldreHarRett).morAleneomsorg(aleneomsorg).build();
        }
        return grunnlagBuilder.morRett(annenForeldreHarRett)
            .farRett(true)
            .farAleneomsorg(aleneomsorg)
            .minsterett(!ref.getSkjæringstidspunkt().utenMinsterett())
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
}
