package no.nav.foreldrepenger.domene.mappers.kalkulatorinput;

import javax.enterprise.inject.Instance;

import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

public interface YtelsesspesifiktGrunnlagMapper {

    static YtelsesspesifiktGrunnlagMapper finnTjeneste(FagsakYtelseType fagsakYtelseType, Instance<YtelsesspesifiktGrunnlagMapper> instanser) {
        return FagsakYtelseTypeRef.Lookup.find(instanser, fagsakYtelseType)
            .orElseThrow(
                () -> new IllegalStateException("Fant ikke implementasjon av YtelsesspesifiktGrunnlagMapper for " + fagsakYtelseType.getKode()));
    }

    YtelsespesifiktGrunnlagDto mapYtelsesspesifiktGrunnlag(BehandlingReferanse behandlingReferanse);

}
