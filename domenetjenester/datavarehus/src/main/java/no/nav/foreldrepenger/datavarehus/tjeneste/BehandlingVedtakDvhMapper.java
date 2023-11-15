package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.domene.BehandlingVedtakDvh;
import no.nav.foreldrepenger.datavarehus.domene.VilkårVerdiDvh;

class BehandlingVedtakDvhMapper {

    private static final Set<VedtakResultatType> VEDTAK_IKKE_OPPFYLT = Set.of(VedtakResultatType.AVSLAG, VedtakResultatType.OPPHØR);

    private BehandlingVedtakDvhMapper() {
    }

    static BehandlingVedtakDvh map(BehandlingVedtak behandlingVedtak, Behandling behandling, LocalDate utbetaltTid, Set<VilkårType> vilkårIkkeOppfylt) {
        return BehandlingVedtakDvh.builder()
                .ansvarligBeslutter(behandling.getAnsvarligBeslutter())
                .ansvarligSaksbehandler(behandlingVedtak.getAnsvarligSaksbehandler())
                .behandlingId(behandling.getId())
                .endretAv(CommonDvhMapper.finnEndretAvEllerOpprettetAv(behandlingVedtak))
                .funksjonellTid(LocalDateTime.now())
                .godkjennendeEnhet(behandling.getBehandlendeEnhet())
                .iverksettingStatus(behandlingVedtak.getIverksettingStatus().getKode())
                .opprettetDato(
                        behandlingVedtak.getOpprettetTidspunkt() == null ? null : behandlingVedtak.getOpprettetTidspunkt().toLocalDate())
                .vedtakDato(behandlingVedtak.getVedtaksdato())
                .vedtakTid(behandlingVedtak.getVedtakstidspunkt())
                .vedtakId(behandlingVedtak.getId())
                .vedtakResultatTypeKode(behandlingVedtak.getVedtakResultatType().getKode())
                .utbetaltTid(utbetaltTid)
                .vilkårIkkeOppfylt(mapVilkårIkkeOppfylt(behandlingVedtak.getVedtakResultatType(), behandling.getFagsakYtelseType(), vilkårIkkeOppfylt))
                .build();
    }

    private static VilkårVerdiDvh mapVilkårIkkeOppfylt(VedtakResultatType vedtakResultatType, FagsakYtelseType ytelseType, Set<VilkårType> vilkårIkkeOppfylt) {
        if (!VEDTAK_IKKE_OPPFYLT.contains(vedtakResultatType)) {
            return null;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.MEDLEMSKAPSVILKÅRET) || vilkårIkkeOppfylt.contains(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE)) {
            return VilkårVerdiDvh.MEDLEMSKAP;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.SØKNADSFRISTVILKÅRET) && vilkårIkkeOppfylt.size() == 1) {
            return VilkårVerdiDvh.SØKNADSFRIST;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.SØKERSOPPLYSNINGSPLIKT) && vilkårIkkeOppfylt.size() == 1) {
            return VilkårVerdiDvh.OPPLYSNINGSPLIKT;
        }
        if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return VilkårVerdiDvh.ENGANSSTØNAD;
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return VilkårVerdiDvh.SVANGERSKAPSPENGER;
        }
        var relasjonTilBarn = vilkårIkkeOppfylt.stream().anyMatch(VilkårType::gjelderRelasjonTilBarn);
        if (relasjonTilBarn) {
            return VilkårVerdiDvh.FORELDREPENGER_GENERELL;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.OPPTJENINGSPERIODEVILKÅR) || vilkårIkkeOppfylt.contains(VilkårType.OPPTJENINGSVILKÅRET)) {
            return VilkårVerdiDvh.FORELDREPENGER_OPPTJENING;
        }
        if (vilkårIkkeOppfylt.contains(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)) {
            return VilkårVerdiDvh.FORELDREPENGER_BEREGNING;
        }
        if (!vilkårIkkeOppfylt.isEmpty()) {
            throw new IllegalStateException("Hvilket tilfelle har vi oversett? " + vilkårIkkeOppfylt);
        }
        return VilkårVerdiDvh.FORELDREPENGER_UTTAK;
    }

}
