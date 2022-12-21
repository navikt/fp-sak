package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSvangerskapspengervilkårDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSvangerskapspengervilkårOppdaterer implements AksjonspunktOppdaterer<BekreftSvangerskapspengervilkårDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    BekreftSvangerskapspengervilkårOppdaterer() {
        //cdi
    }

    @Inject
    public BekreftSvangerskapspengervilkårOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                     BehandlingRepositoryProvider repositoryProvider) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    @Override
    public OppdateringResultat oppdater(BekreftSvangerskapspengervilkårDto dto, AksjonspunktOppdaterParameter param) {
        var vilkårOppfylt = dto.getAvslagskode() == null;
        lagHistorikkinnslag(dto.getBegrunnelse(), vilkårOppfylt);
        if (vilkårOppfylt) {
            return OppdateringResultat.utenTransisjon()
                .leggTilManueltOppfyltVilkår(VilkårType.SVANGERSKAPSPENGERVILKÅR).build();
        } else {
            var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));
            var ref = param.getRef();
            var behandling = behandlingRepository.hentBehandling(ref.behandlingUuid());
            var transisjon = FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;
            if (behandling.erRevurdering() && !harAvslåttForrigeBehandling(behandling)) {
                transisjon = FellesTransisjoner.FREMHOPP_TIL_UTTAKSPLAN;
            }
            return new OppdateringResultat.Builder()
                .medFremoverHopp(transisjon)
                .medTotrinn()
                .leggTilManueltAvslåttVilkår(VilkårType.SVANGERSKAPSPENGERVILKÅR, avslagsårsak)
                .build();
        }
    }
    private boolean harAvslåttForrigeBehandling(Behandling revurdering) {
        var originalBehandling = revurdering.getOriginalBehandlingId().map(behandlingRepository::hentBehandling);
        if (originalBehandling.isPresent()) {
            var behandlingsresultat = getBehandlingsresultat(originalBehandling.get());
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det
            // faktiske resultatet for å kunne vurdere om forrige behandling var avslått
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultat.getBehandlingResultatType())) {
                return harAvslåttForrigeBehandling(originalBehandling.get());
            }
            return behandlingsresultat.isBehandlingsresultatAvslått();
        }
        return false;
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hent(behandling.getId());
    }
    private void lagHistorikkinnslag(String begrunnelse,
                                     boolean vilkårOppfylt) {
        var tilVerdi = vilkårOppfylt ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;

        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse)
            .medEndretFelt(HistorikkEndretFeltType.SVANGERSKAPSPENGERVILKÅRET, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_SVANGERSKAPSPENGER);
    }
}
