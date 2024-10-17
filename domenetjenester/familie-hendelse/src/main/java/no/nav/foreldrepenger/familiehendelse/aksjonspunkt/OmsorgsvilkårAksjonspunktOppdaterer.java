package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår1AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår2AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.exception.FunksjonellException;

/**
 * Håndterer oppdatering av omsorgsvilkåret.
 */
public abstract class OmsorgsvilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<AvslagbartAksjonspunktDto> {

    private VilkårType vilkårType;
    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingRepository bRepo;

    protected OmsorgsvilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    public OmsorgsvilkårAksjonspunktOppdaterer(HistorikkTjenesteAdapter historikkAdapter, VilkårType vilkårType,
                                               BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.vilkårType = vilkårType;
        this.bRepo = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvslagbartAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = bRepo.hentBehandling(param.getBehandlingId());
        var resultatBuilder = OppdateringResultat.utenTransisjon();
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        var skjermlenkeType = HistorikkAksjonspunktAdapter.getSkjermlenkeType(vilkårType, aksjonspunktDefinisjon);
        historikkAdapter.tekstBuilder()
            .medEndretFelt(getTekstKode(), null, dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.OPPFYLT : HistorikkEndretFeltVerdiType.IKKE_OPPFYLT)
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(skjermlenkeType);

        // Rydd opp gjenopprettede aksjonspunkt på andre omsorgsvilkår ved eventuelt tilbakehopp
        behandling.getAksjonspunkter().stream()
            .filter(ap -> OmsorgsvilkårKonfigurasjon.OMSORGS_AKSJONSPUNKT.contains(ap.getAksjonspunktDefinisjon()))
            .filter(ap -> !Objects.equals(ap.getAksjonspunktDefinisjon(), aksjonspunktDefinisjon)) // ikke sett seg selv til avbrutt
            .filter(Aksjonspunkt::erOpprettet)
            .forEach(ap -> resultatBuilder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));

        if (dto.getErVilkarOk()) {
            return resultatBuilder.leggTilManueltOppfyltVilkår(vilkårType).build();
        } else {
            var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));
            return resultatBuilder
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(vilkårType, avslagsårsak)
                .build();
        }
    }

    protected abstract HistorikkEndretFeltType getTekstKode();

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = Foreldreansvarsvilkår1AksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class Foreldreansvarsvilkår1Oppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        Foreldreansvarsvilkår1Oppdaterer() {
            // for CDI proxy
        }

        @Inject
        public Foreldreansvarsvilkår1Oppdaterer(HistorikkTjenesteAdapter historikkAdapter, BehandlingRepository behandlingRepository) {
            super(historikkAdapter, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, behandlingRepository);
        }

        @Override
        protected HistorikkEndretFeltType getTekstKode() {
            return HistorikkEndretFeltType.FORELDREANSVARSVILKARET;
        }

    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = Foreldreansvarsvilkår2AksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class Foreldreansvarsvilkår2Oppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        Foreldreansvarsvilkår2Oppdaterer() {
            // for CDI proxy
        }

        @Inject
        public Foreldreansvarsvilkår2Oppdaterer(HistorikkTjenesteAdapter historikkAdapter, BehandlingRepository behandlingRepository) {
            super(historikkAdapter, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, behandlingRepository);
        }

        @Override
        protected HistorikkEndretFeltType getTekstKode() {
            return HistorikkEndretFeltType.FORELDREANSVARSVILKARET;
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = OmsorgsvilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class OmsorgsvilkårOppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        OmsorgsvilkårOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public OmsorgsvilkårOppdaterer(HistorikkTjenesteAdapter historikkAdapter, BehandlingRepository behandlingRepository) {
            super(historikkAdapter, VilkårType.OMSORGSVILKÅRET, behandlingRepository);
        }

        @Override
        protected HistorikkEndretFeltType getTekstKode() {
            return HistorikkEndretFeltType.OMSORGSVILKAR;
        }

    }
}
