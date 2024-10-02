package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.domene.VilkårIkkeOppfylt;

public class BehandlingVedtakDvhMapper {

    private static final Set<VedtakResultatType> VEDTAK_IKKE_OPPFYLT = Set.of(VedtakResultatType.AVSLAG, VedtakResultatType.OPPHØR);

    private BehandlingVedtakDvhMapper() {
    }

    public static VilkårIkkeOppfylt mapVilkårIkkeOppfylt(VedtakResultatType vedtakResultatType, FagsakYtelseType ytelseType, Set<VilkårType> vilkårIkkeOppfylt) {
        if (vedtakResultatType == null || !VEDTAK_IKKE_OPPFYLT.contains(vedtakResultatType)) {
            return null;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.MEDLEMSKAPSVILKÅRET) || vilkårIkkeOppfylt.contains(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE)
            || vilkårIkkeOppfylt.contains(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE)) {
            return VilkårIkkeOppfylt.MEDLEMSKAP;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.SØKNADSFRISTVILKÅRET) && vilkårIkkeOppfylt.size() == 1) {
            return VilkårIkkeOppfylt.SØKNADSFRIST;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.SØKERSOPPLYSNINGSPLIKT) && vilkårIkkeOppfylt.size() == 1) {
            return VilkårIkkeOppfylt.OPPLYSNINGSPLIKT;
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return VilkårIkkeOppfylt.ENGANGSSTØNAD;
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return VilkårIkkeOppfylt.SVANGERSKAPSPENGER;
        }
        var relasjonTilBarn = vilkårIkkeOppfylt.stream().anyMatch(VilkårType::gjelderRelasjonTilBarn);
        if (relasjonTilBarn) {
            return VilkårIkkeOppfylt.FORELDREPENGER_GENERELL;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.OPPTJENINGSPERIODEVILKÅR) || vilkårIkkeOppfylt.contains(VilkårType.OPPTJENINGSVILKÅRET)) {
            return VilkårIkkeOppfylt.FORELDREPENGER_OPPTJENING;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)) {
            return VilkårIkkeOppfylt.FORELDREPENGER_BEREGNING;
        }
        if (!vilkårIkkeOppfylt.isEmpty()) {
            throw new IllegalStateException("Hvilket tilfelle har vi oversett? " + vilkårIkkeOppfylt);
        }
        return VilkårIkkeOppfylt.FORELDREPENGER_UTTAK;
    }

}
