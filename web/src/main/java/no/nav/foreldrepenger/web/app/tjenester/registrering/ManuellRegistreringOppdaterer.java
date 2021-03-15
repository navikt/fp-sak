package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.xml.sax.SAXException;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.mottak.registrerer.DokumentRegistrererTjeneste;
import no.nav.foreldrepenger.mottak.registrerer.ManuellRegistreringAksjonspunktDto;
import no.nav.foreldrepenger.søknad.v3.SøknadConstants;
import no.nav.foreldrepenger.web.app.tjenester.registrering.svp.ManuellRegistreringSvangerskapspengerDto;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.v3.ObjectFactory;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;
import no.nav.vedtak.xmlutils.JaxbHelper;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ManuellRegistreringDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManuellRegistreringOppdaterer implements AksjonspunktOppdaterer<ManuellRegistreringDto> {

    private FagsakRepository fagsakRepository;
    private HistorikkTjenesteAdapter historikkApplikasjonTjeneste;
    private DokumentRegistrererTjeneste dokumentRegistrererTjeneste;

    private Instance<SøknadMapper> søknadMappere;

    ManuellRegistreringOppdaterer() {
        // CDI
    }

    @Inject
    public ManuellRegistreringOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                         HistorikkTjenesteAdapter historikkApplikasjonTjeneste,
                                         DokumentRegistrererTjeneste dokumentRegistrererTjeneste,
                                         @Any Instance<SøknadMapper> søknadMappere) {
        this.søknadMappere = søknadMappere;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.historikkApplikasjonTjeneste = historikkApplikasjonTjeneste;
        this.dokumentRegistrererTjeneste = dokumentRegistrererTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(ManuellRegistreringDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        oppdaterBehandlingDersomMigrert(behandling, dto);
        Long behandlingId = param.getBehandlingId();
        OppdateringResultat.Builder resultatBuilder = OppdateringResultat.utenTransisjon();

        if (dto.getUfullstendigSoeknad()) {

            final ManuellRegistreringAksjonspunktDto adapter = new ManuellRegistreringAksjonspunktDto(!dto.getUfullstendigSoeknad());
            dokumentRegistrererTjeneste.aksjonspunktManuellRegistrering(behandling, adapter)
                .ifPresent(ad -> resultatBuilder.medEkstraAksjonspunktResultat(ad, AksjonspunktStatus.OPPRETTET));
            lagHistorikkInnslag(behandlingId, HistorikkinnslagType.MANGELFULL_SØKNAD, null);
            return resultatBuilder
                .leggTilVilkårResultat(VilkårType.SØKERSOPPLYSNINGSPLIKT, VilkårUtfallType.IKKE_VURDERT)
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_TIL_KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT).build();
        }

        ManuellRegistreringValidator.validerOpplysninger(dto);

        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(behandling.getFagsakId());
        NavBruker navBruker = fagsak.getNavBruker();
        String søknadXml = opprettSøknadsskjema(dto, behandling, navBruker);
        DokumentTypeId dokumentTypeId = finnDokumentType(dto, behandling.getType());

        final ManuellRegistreringAksjonspunktDto adapter = new ManuellRegistreringAksjonspunktDto(!dto.getUfullstendigSoeknad(), søknadXml,
            dokumentTypeId, dto.getMottattDato(), dto.isRegistrerVerge());
        dokumentRegistrererTjeneste.aksjonspunktManuellRegistrering(behandling, adapter)
            .ifPresent(ad -> resultatBuilder.medEkstraAksjonspunktResultat(ad, AksjonspunktStatus.OPPRETTET));

        lagHistorikkInnslag(behandlingId, HistorikkinnslagType.REGISTRER_PAPIRSØK, dto.getKommentarEndring());
        if (dto instanceof ManuellRegistreringSvangerskapspengerDto) {
            var svpDto = (ManuellRegistreringSvangerskapspengerDto) dto;
            if (svpDto.isMigrertFraInfotrygd()) {
                lagHistorikkInnslag(behandlingId, HistorikkinnslagType.MIGRERT_FRA_INFOTRYGD, null);
            }
        }
        return resultatBuilder.build();

    }

    private void oppdaterBehandlingDersomMigrert(Behandling behandling, ManuellRegistreringDto dto) {
        if (dto instanceof ManuellRegistreringSvangerskapspengerDto) {
            var manuellRegistreringSvangerskapspengerDto = (ManuellRegistreringSvangerskapspengerDto) dto;
            if (manuellRegistreringSvangerskapspengerDto.isMigrertFraInfotrygd()) {
                behandling.setMigrertKilde(Fagsystem.INFOTRYGD);
            }
        }
    }

    private DokumentTypeId finnDokumentType(ManuellRegistreringDto dto, BehandlingType behandlingType) {
        String søknadsType = dto.getSoknadstype().getKode();

        if (FagsakYtelseType.ENGANGSTØNAD.getKode().equals(søknadsType)) {
            if (erFødsel(dto)) {
                return DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL;
            }
            if (erAdopsjon(dto)) {
                return DokumentTypeId.SØKNAD_ENGANGSSTØNAD_ADOPSJON;
            }
        } else if (FagsakYtelseType.FORELDREPENGER.getKode().equals(søknadsType)) {
            if (erEndringssøknad(behandlingType)) {
                return DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD;
            }
            if (erFødsel(dto)) {
                return DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL;
            }
            if (erAdopsjon(dto)) {
                return DokumentTypeId.SØKNAD_FORELDREPENGER_ADOPSJON;
            }
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.getKode().equals(søknadsType)) {
            return DokumentTypeId.SØKNAD_SVANGERSKAPSPENGER;

        }
        return DokumentTypeId.UDEFINERT;
    }

    private boolean erEndringssøknad(BehandlingType behandlingType) {
        return behandlingType.equals(BehandlingType.REVURDERING);
    }

    private boolean erAdopsjon(ManuellRegistreringDto dto) {
        return (FamilieHendelseType.ADOPSJON.getKode().equals(dto.getTema().getKode()) ||
                FamilieHendelseType.OMSORG.getKode().equals(dto.getTema().getKode()));
    }

    private boolean erFødsel(ManuellRegistreringDto dto) {
        return FamilieHendelseType.FØDSEL.getKode().equals(dto.getTema().getKode());
    }

    private String opprettSøknadsskjema(ManuellRegistreringDto dto, Behandling behandling, NavBruker navBruker) {
        Soeknad søknad = null;

        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        BehandlingType behandlingType = behandling.getType();


        if (dto.getKode().equals(REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER.getKode())) {
            // minihack for
            behandlingType = BehandlingType.REVURDERING;;
        }

        SøknadMapper mapper = finnSøknadMapper(ytelseType, behandlingType);
        søknad = mapper.mapSøknad(dto, navBruker);

        String søknadXml;
        try {
            søknadXml = JaxbHelper.marshalAndValidateJaxb(SøknadConstants.JAXB_CLASS,
                new ObjectFactory().createSoeknad(søknad),
                SøknadConstants.XSD_LOCATION,
                SøknadConstants.ADDITIONAL_XSD_LOCATION,
                SøknadConstants.ADDITIONAL_CLASSES);
        } catch (JAXBException | SAXException e) {
            throw new TekniskException("FP-453254", "Feil ved marshalling av søknadsskjema", e);
        }
        return søknadXml;
    }

    private void lagHistorikkInnslag(Long behandlingId, HistorikkinnslagType innslagType, String kommentarEndring) {
        Historikkinnslag innslag = new Historikkinnslag();
        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder();

        innslag.setAktør(HistorikkAktør.SAKSBEHANDLER);
        innslag.setBehandlingId(behandlingId);
        innslag.setType(innslagType);
        builder.medHendelse(innslagType);
        if (kommentarEndring != null) {
            builder.medBegrunnelse(kommentarEndring);
        }
        builder.build(innslag);
        historikkApplikasjonTjeneste.lagInnslag(innslag);
    }

    public SøknadMapper finnSøknadMapper(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(SøknadMapper.class, søknadMappere, ytelseType, behandlingType).orElseThrow();
    }
}
