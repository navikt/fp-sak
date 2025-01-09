package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår1AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.Foreldreansvarsvilkår2AksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.OmsorgsvilkårAksjonspunktDto;
import no.nav.vedtak.exception.FunksjonellException;

/**
 * Håndterer oppdatering av omsorgsvilkåret.
 */
public abstract class OmsorgsvilkårAksjonspunktOppdaterer implements AksjonspunktOppdaterer<AvslagbartAksjonspunktDto> {

    private VilkårType vilkårType;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private BehandlingRepository bRepo;

    protected OmsorgsvilkårAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    public OmsorgsvilkårAksjonspunktOppdaterer(HistorikkinnslagRepository historikkinnslagRepository, VilkårType vilkårType,
                                               BehandlingRepository behandlingRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
        this.vilkårType = vilkårType;
        this.bRepo = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(AvslagbartAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var behandling = bRepo.hentBehandling(ref.behandlingId());
        var resultatBuilder = OppdateringResultat.utenTransisjon();
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        lagHistorikkinnslag(dto, ref, aksjonspunktDefinisjon);

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

    private void lagHistorikkinnslag(AvslagbartAksjonspunktDto dto, BehandlingReferanse ref, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var fraVerdiNavn = switch (vilkårType) {
            case FORELDREANSVARSVILKÅRET_2_LEDD, FORELDREANSVARSVILKÅRET_4_LEDD -> "Foreldreansvarsvilkåret";
            case OMSORGSVILKÅRET -> "Omsorgsvilkåret";
            default -> throw new IllegalStateException("Oppdaterer skal ikke være brukt av andre vilkårtyper, men ble brukt av " + vilkårType);
        };
        var historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medFagsakId(ref.fagsakId())
            .medBehandlingId(ref.behandlingId())
            .medTittel(HistorikkSammeBarnTjeneste.getSkjermlenkeType(vilkårType, aksjonspunktDefinisjon))
            .addLinje(fraTilEquals(fraVerdiNavn, null, dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = Foreldreansvarsvilkår1AksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class Foreldreansvarsvilkår1Oppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        Foreldreansvarsvilkår1Oppdaterer() {
            // for CDI proxy
        }

        @Inject
        public Foreldreansvarsvilkår1Oppdaterer(HistorikkinnslagRepository historikkinnslagRepository, BehandlingRepository behandlingRepository) {
            super(historikkinnslagRepository, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, behandlingRepository);
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = Foreldreansvarsvilkår2AksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class Foreldreansvarsvilkår2Oppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        Foreldreansvarsvilkår2Oppdaterer() {
            // for CDI proxy
        }

        @Inject
        public Foreldreansvarsvilkår2Oppdaterer(HistorikkinnslagRepository historikkinnslagRepository, BehandlingRepository behandlingRepository) {
            super(historikkinnslagRepository, VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, behandlingRepository);
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = OmsorgsvilkårAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class OmsorgsvilkårOppdaterer extends OmsorgsvilkårAksjonspunktOppdaterer {

        OmsorgsvilkårOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public OmsorgsvilkårOppdaterer(HistorikkinnslagRepository historikkinnslagRepository, BehandlingRepository behandlingRepository) {
            super(historikkinnslagRepository, VilkårType.OMSORGSVILKÅRET, behandlingRepository);
        }

    }
}
