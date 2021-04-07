package no.nav.foreldrepenger.domene.vedtak.svp;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.domene.vedtak.xml.VedtakXmlUtil;
import no.nav.foreldrepenger.domene.vedtak.xml.VilkårsgrunnlagXmlTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.fødsel.FødselsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.PersonStatusType;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.Opptjeningsgrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.søknadsfrist.SoeknadsfristvilkarGrunnlag;
import no.nav.foreldrepenger.kompletthet.KompletthetsjekkerProvider;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.felles.xml.felles.v2.DateOpplysning;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.svp.v2.ObjectFactory;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.svp.v2.VilkaarsgrunnlagFoedsel;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.svp.v2.VilkaarsgrunnlagMedlemskap;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.svp.v2.VilkaarsgrunnlagOpptjening;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.svp.v2.VilkaarsgrunnlagSoekersopplysningsplikt;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.svp.v2.VilkaarsgrunnlagSoeknadsfrist;
import no.nav.vedtak.felles.xml.vedtak.vilkaarsgrunnlag.v2.Vilkaarsgrunnlag;

@FagsakYtelseTypeRef("SVP")
@ApplicationScoped
public class VilkårsgrunnlagXmlTjenesteImpl extends VilkårsgrunnlagXmlTjeneste {
    private ObjectFactory vilkårObjectFactory = new ObjectFactory();
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    public VilkårsgrunnlagXmlTjenesteImpl() {
        //For CDI
    }

    @Inject
    public VilkårsgrunnlagXmlTjenesteImpl(BehandlingRepositoryProvider repositoryProvider, KompletthetsjekkerProvider kompletthetsjekkerProvider, SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        super(repositoryProvider, kompletthetsjekkerProvider);
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    protected Vilkaarsgrunnlag getVilkaarsgrunnlag(Behandling behandling, Vilkår vilkårFraBehandling, Optional<SøknadEntitet> søknad) {
        //TODO PFP-7642 Implementere basert på VilkårsgrunnlagXmlTjenesteForeldrepenger
        Vilkaarsgrunnlag vilkaarsgrunnlag = null;
        if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårFraBehandling.getVilkårType()) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForFødselsvilkåret(behandling, vilkårFraBehandling);
        } else if (VilkårType.SØKNADSFRISTVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøknadsfristvilkåret(vilkårFraBehandling);
        } else if (VilkårType.SØKERSOPPLYSNINGSPLIKT.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForSøkersopplysningsplikt(behandling, søknad);
        } else if (VilkårType.MEDLEMSKAPSVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForMedlemskapsvilkåret(vilkårFraBehandling);
        } else if (VilkårType.OPPTJENINGSVILKÅRET.equals(vilkårFraBehandling.getVilkårType())) {
            vilkaarsgrunnlag = lagVilkaarsgrunnlagForOpptjening(vilkårFraBehandling);
        }

        return vilkaarsgrunnlag;
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
        vilkårgrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isElektroniskSoeknad()));
        VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getSoeknadMottatDato())
            .ifPresent(vilkårgrunnlag::setSoeknadMottattDato);
        VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getSkjaeringstidspunkt())
            .ifPresent(vilkårgrunnlag::setSkjaeringstidspunkt);

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForFødselsvilkåret(Behandling behandling, Vilkår vilkårFraBehandling) {
        VilkaarsgrunnlagFoedsel vilkårgrunnlagFødselForeldrepenger = vilkårObjectFactory.createVilkaarsgrunnlagFoedsel();
        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlagFødselForeldrepenger;
        }
        FødselsvilkårGrunnlag grunnlagForVilkår = getObjectMapper().readValue(
            vilkårFraBehandling.getRegelInput(),
            FødselsvilkårGrunnlag.class
        );
        vilkårgrunnlagFødselForeldrepenger.setAntallBarn(VedtakXmlUtil.lagIntOpplysning(grunnlagForVilkår.getAntallBarn()));
        Optional<DateOpplysning> bekreftetFødselsdato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getBekreftetFoedselsdato());
        bekreftetFødselsdato.ifPresent(vilkårgrunnlagFødselForeldrepenger::setFoedselsdatoBarn);

        if (grunnlagForVilkår.getSoekerRolle() != null) {
            vilkårgrunnlagFødselForeldrepenger.setSoekersRolle(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.getSoekerRolle().getKode()));
        }
        Optional<DateOpplysning> søknadDato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getDagensdato());
        søknadDato.ifPresent(vilkårgrunnlagFødselForeldrepenger::setSoeknadsdato);

        vilkårgrunnlagFødselForeldrepenger.setSokersKjoenn(VedtakXmlUtil.lagStringOpplysning(grunnlagForVilkår.getSoekersKjonn().name()));

        Optional<DateOpplysning> bekreftetTerminDato = VedtakXmlUtil.lagDateOpplysning(grunnlagForVilkår.getBekreftetTermindato());
        bekreftetTerminDato.ifPresent(vilkårgrunnlagFødselForeldrepenger::setTermindato);

        final FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag = familieHendelseRepository.hentAggregat(behandling.getId());

        if (Objects.nonNull(familieHendelseGrunnlag.getGjeldendeVersjon().erMorForSykVedFødsel())) {
            vilkårgrunnlagFødselForeldrepenger.setErMorForSykVedFodsel(VedtakXmlUtil.lagBooleanOpplysning(familieHendelseGrunnlag.getGjeldendeVersjon().erMorForSykVedFødsel()));
        }

        return vilkårgrunnlagFødselForeldrepenger;
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
        vilkårgrunnlag.setOppholdstillatelse(VedtakXmlUtil.lagBooleanOpplysning(grunnlagForVilkår.isBrukerHarOppholdstillatelse()));
        vilkårgrunnlag.setPersonstatus(VedtakXmlUtil.lagStringOpplysning(
            Optional.ofNullable(grunnlagForVilkår.getPersonStatusType()).map(PersonStatusType::getKode).orElse("-")
        ));
        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForOpptjening(Vilkår vilkårFraBehandling) {
        VilkaarsgrunnlagOpptjening vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagOpptjening();

        if (vilkårFraBehandling.getRegelInput() == null) {
            return vilkårgrunnlag;
        }

        Opptjeningsgrunnlag opptjeningsgrunnlag = getObjectMapper().readValue(
            vilkårFraBehandling.getRegelInput(),
            Opptjeningsgrunnlag.class
        );

        if (opptjeningsgrunnlag != null) {
            VedtakXmlUtil.lagDateOpplysning(opptjeningsgrunnlag.getBehandlingsTidspunkt()).ifPresent(vilkårgrunnlag::setBehandlingsDato);

            vilkårgrunnlag.setMinsteAntallDagerGodkjent(VedtakXmlUtil.lagIntOpplysning(opptjeningsgrunnlag.getMinsteAntallDagerGodkjent()));
            vilkårgrunnlag.setMinsteAntallMånederGodkjent(VedtakXmlUtil.lagIntOpplysning(opptjeningsgrunnlag.getMinsteAntallMånederGodkjent()));
            LocalDateInterval opptjeningsperiode = opptjeningsgrunnlag.getOpptjeningPeriode();
            vilkårgrunnlag.setOpptjeningperiode(VedtakXmlUtil.lagPeriodeOpplysning(opptjeningsperiode.getFomDato(), opptjeningsperiode.getTomDato()));
            vilkårgrunnlag.setMinsteInntekt(VedtakXmlUtil.lagLongOpplysning(opptjeningsgrunnlag.getMinsteInntekt()));

            vilkårgrunnlag.setMaksMellomliggendePeriodeForArbeidsforhold(VedtakXmlUtil.lagStringOpplysningForperiode(opptjeningsgrunnlag.getMaksMellomliggendePeriodeForArbeidsforhold()));
            vilkårgrunnlag.setMinForegaaendeForMellomliggendePeriodeForArbeidsforhold(VedtakXmlUtil.lagStringOpplysningForperiode(opptjeningsgrunnlag.getMinForegåendeForMellomliggendePeriodeForArbeidsforhold()));
            vilkårgrunnlag.setPeriodeAntattGodkjentForBehandlingstidspunkt(VedtakXmlUtil.lagStringOpplysningForperiode(opptjeningsgrunnlag.getPeriodeAntattGodkjentFørBehandlingstidspunkt()));
        }

        return vilkårgrunnlag;
    }

    private Vilkaarsgrunnlag lagVilkaarsgrunnlagForSøkersopplysningsplikt(Behandling behandling, Optional<SøknadEntitet> optionalSøknad) {
        boolean elektroniskSøknad;
        LocalDate mottattDato;
        LocalDate skjæringstidspunkt;
        if (!optionalSøknad.isPresent()) {
            elektroniskSøknad = false;
            mottattDato = null;
            skjæringstidspunkt = null;
        } else {
            SøknadEntitet søknad = optionalSøknad.get();
            mottattDato = getMottattDato(behandling);
            elektroniskSøknad = søknad.getElektroniskRegistrert();
            skjæringstidspunkt = getSkjæringstidsunkt(behandling.getId());
        }
        VilkaarsgrunnlagSoekersopplysningsplikt vilkårgrunnlag = vilkårObjectFactory.createVilkaarsgrunnlagSoekersopplysningsplikt();

        vilkårgrunnlag.setElektroniskSoeknad(VedtakXmlUtil.lagBooleanOpplysning(elektroniskSøknad));
        VedtakXmlUtil.lagDateOpplysning(mottattDato).ifPresent(vilkårgrunnlag::setSoeknadMottatDato);
        VedtakXmlUtil.lagDateOpplysning(skjæringstidspunkt).ifPresent(vilkårgrunnlag::setSkjaeringstidspunkt);
        return vilkårgrunnlag;
    }

    private LocalDate getSkjæringstidsunkt(Long behandlingId) {
        return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getUtledetSkjæringstidspunkt();
    }
}
