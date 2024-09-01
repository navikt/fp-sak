package no.nav.foreldrepenger.domene.mappers.input;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.SvangerskapspengerGrunnlag;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.BeregnTilrettleggingsperioderTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class MapKalkulusYtelsegrunnlagSVP implements MapKalkulusYtelsegrunnlag {
    private BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste;

    MapKalkulusYtelsegrunnlagSVP() {
        // CDI
    }

    @Inject
    public MapKalkulusYtelsegrunnlagSVP(BeregnTilrettleggingsperioderTjeneste tilrettleggingsperioderTjeneste) {
        this.tilrettleggingsperioderTjeneste = tilrettleggingsperioderTjeneste;
    }

    @Override
    public YtelsespesifiktGrunnlagDto mapYtelsegrunnlag(BehandlingReferanse referanse, Skjæringstidspunkt stp) {
        var tilretteleggingMedUtbelingsgrad = tilrettleggingsperioderTjeneste.beregnPerioder(referanse);
        return new SvangerskapspengerGrunnlag(MapTilrettelegginger.mapTilretteleggingerMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad), Tid.TIDENES_ENDE);
    }
}
