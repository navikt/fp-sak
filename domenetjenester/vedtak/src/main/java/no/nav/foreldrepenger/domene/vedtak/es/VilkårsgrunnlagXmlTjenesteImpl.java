package no.nav.foreldrepenger.domene.vedtak.es;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.vedtak.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.adopsjon.AdopsjonsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.PersonStatusType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.SoeknadsfristvilkarGrunnlag;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.felles.xml.felles.v2.DateOpplysning;
import no.nav.vedtak.felles.xml.vedtak.personopplysninger.es.v2.Adopsjon;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.VilkaarsgrunnlagAdopsjon;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.VilkaarsgrunnlagFoedsel;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.VilkaarsgrunnlagMedlemskap;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.VilkaarsgrunnlagSoekersopplysningsplikt;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.es.v2.VilkaarsgrunnlagSoeknadsfrist;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.Vilkaarsgrunnlag;

@FagsakYtelseTypeRef("ES")
@ApplicationScoped
public class VilkårsgrunnlagXmlTjenesteImpl extends VilkårsgrunnlagXmlTjeneste {

    private ObjectFactory vilkårObjectFactory = new ObjectFactory();
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VilkårsgrunnlagXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public VilkårsgrunnlagXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider,
                                                   KompletthetsjekkerProvider kompletthetsjekkerProvider,
                                                   SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, kompletthetsjekkerProvider);
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;

    }

    @Override
    protected Vilkaarsgrunnlag getVilkaarsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling, Optional<SøknadEntitet> søknad) {
        Vilkaarsgrunnlag vilkaarsgrunnlag = null;
        if (VilkårType.SØKERSOPPLYSNINGSPLIKT.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøkersopplysningsplikt(behandling, søknad);
        } else if (VilkårType.MEDLEMSKAPSVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForMedlemskapsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårFraBehandling.getVilkårType()) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForFødselsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.SØKNADSFRISTVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøknadsfristvilkåret(vilkårFraBehandling);
        } else if ((VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.equals(vilkårFraBehandling.getVilkårType())) || (VilkårType.ADOPSJONSVILKARET_FORELDREPENGER.equals(vilkårFraBehandling.getVilkårType()))) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForAdopsjonsvilkåret(vilkårFraBehandling);
        }
        return vilkaarsgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForAdopsjonsvilkåret(Vilkår vilkårFraBehandling) {
        VilkaarsgrunnlagAdopsjon vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagAdopsjon();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        AdopsjonsvilkårGrunnlag grunnlagForVilkår = getObjectMapper().readValue(
            vilkårFraBehandling.getRegelInput(),
            AdopsjonsvilkårGrunnlag.class
        );
        vilkårgrunnlag.setSoekersKjoenn(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.getSoekersKjonn().name()));
        Adopsjon adopsjon = new Adopsjon();

        Optional<DateOpplysning> omsorgOvertakelseDato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getOmsorgsovertakelsesdato());
        omsorgOvertakelseDato.ifPresent(adopsjon::setOmsorgsovertakelsesdato);

        adopsjon.setErMannAdoptererAlene(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isMannAdoptererAlene()));

        adopsjon.setErEktefellesBarn(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isEktefellesBarn()));
        vilkårgrunnlag.setAdopsjon(adopsjon);
        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForSøknadsfristvilkåret(Vilkår vilkårFraBehandling) {
        VilkaarsgrunnlagSoeknadsfrist vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoeknadsfrist();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        SoeknadsfristvilkarGrunnlag grunnlagForVilkår = getObjectMapper().readValue(
            vilkårFraBehandling.getRegelInput(),
            SoeknadsfristvilkarGrunnlag.class
        );
        VilkaarsgrunnlagSoeknadsfrist vilkargrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoeknadsfrist();
        vilkargrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isElektroniskSoeknad()));

        Optional<DateOpplysning> skjæringstidspunkt = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getSkjaeringstidspunkt());
        skjæringstidspunkt.ifPresent(vilkargrunnlag::setSkjaeringstidspunkt);

        Optional<DateOpplysning> søknadMotattDato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getSoeknadMottatDato());
        søknadMotattDato.ifPresent(vilkargrunnlag::setSoeknadMottattDato);

        return vilkargrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForFødselsvilkåret(Vilkår vilkårFraBehandling) {
        VilkaarsgrunnlagFoedsel vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagFoedsel();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        FødselsvilkårGrunnlag grunnlagForVilkår = getObjectMapper().readValue(
            vilkårFraBehandling.getRegelInput(),
            FødselsvilkårGrunnlag.class
        );
        vilkårgrunnlag.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(grunnlagForVilkår.getAntallBarn()));
        Optional<DateOpplysning> bekreftetFødselsdato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getBekreftetFoedselsdato());
        bekreftetFødselsdato.ifPresent(vilkårgrunnlag::setFoedselsdatoBarn);

        if (grunnlagForVilkår.getSoekerRolle() != null) {
            vilkårgrunnlag.setSoekersRolle(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.getSoekerRolle().getKode()));
        }
        Optional<DateOpplysning> søknadDato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getDagensdato());
        søknadDato.ifPresent(vilkårgrunnlag::setSoeknadsdato);

        vilkårgrunnlag.setSokersKjoenn(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.getSoekersKjonn().name()));

        Optional<DateOpplysning> bekreftetTerminDato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getBekreftetTermindato());
        bekreftetTerminDato.ifPresent(vilkårgrunnlag::setTermindato);

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForMedlemskapsvilkåret(Vilkår vilkårFraBehandling) {
        VilkaarsgrunnlagMedlemskap vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagMedlemskap();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }
        MedlemskapsvilkårGrunnlag grunnlagForVilkår = getObjectMapper().readValue(
            vilkårFraBehandling.getRegelInput(),
            MedlemskapsvilkårGrunnlag.class
        );
        vilkårgrunnlag.setErBrukerBorgerAvEUEOS(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerBorgerAvEUEOS()));
        vilkårgrunnlag.setHarBrukerLovligOppholdINorge(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerAvklartLovligOppholdINorge()));
        vilkårgrunnlag.setHarBrukerOppholdsrett(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerAvklartOppholdsrett()));
        vilkårgrunnlag.setErBrukerBosatt(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerAvklartBosatt()));
        vilkårgrunnlag.setErBrukerNordiskstatsborger(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerNorskNordisk()));
        vilkårgrunnlag.setErBrukerPliktigEllerFrivilligMedlem(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerAvklartPliktigEllerFrivillig()));
        vilkårgrunnlag.setErBrukerMedlem(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerErMedlem()));
        vilkårgrunnlag.setPersonstatus(VedtakXmlUtil.lagStringOpplysning(
            Optional.ofNullable(grunnlagForVilkår.getPersonStatusType()).map(PersonStatusType::getKode).orElse("-")));

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForSøkersopplysningsplikt(Behandling behandling, Optional<SøknadEntitet> optionalSøknad) {
        boolean komplettSøknad;
        boolean elektroniskSøknad;
        boolean erBarnetFødt;
        if (!optionalSøknad.isPresent()) {
            komplettSøknad = false;
            elektroniskSøknad = false;
            erBarnetFødt = false;
        } else {
            var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            SøknadEntitet søknad = optionalSøknad.get();
            komplettSøknad = erKomplettSøknad(ref);
            elektroniskSøknad = søknad.getElektroniskRegistrert();
            erBarnetFødt = erBarnetFødt(behandling);
        }
        VilkaarsgrunnlagSoekersopplysningsplikt vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoekersopplysningsplikt();
        vilkårgrunnlag.setErSoeknadenKomplett(VedtakXmlUtil.lagBooleanOpplysning(komplettSøknad)); //Denne er unødvendig fo dvh.
        vilkårgrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(elektroniskSøknad));
        vilkårgrunnlag.setErBarnetFoedt(VedtakXmlUtil.lagBooleanOpplysning(erBarnetFødt));
        return vilkårgrunnlag;
    }
}
