package no.nav.foreldrepenger.behandling.steg.avklarfakta.es;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtleder;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktUtlederResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.YtelserSammeBarnTjeneste;

@ApplicationScoped
class AksjonspunktUtlederForTidligereMottattYtelse implements AksjonspunktUtleder {

    private static final List<AksjonspunktUtlederResultat> INGEN_AKSJONSPUNKTER = List.of();

    private PersonopplysningRepository personopplysningRepository;
    private YtelserSammeBarnTjeneste ytelseTjeneste;

    // For CDI.
    AksjonspunktUtlederForTidligereMottattYtelse() {
    }

    @Inject
    AksjonspunktUtlederForTidligereMottattYtelse(YtelserSammeBarnTjeneste ytelseTjeneste,
            PersonopplysningRepository personopplysningRepository) {
        this.personopplysningRepository = personopplysningRepository;
        this.ytelseTjeneste = ytelseTjeneste;
    }

    @Override
    public List<AksjonspunktUtlederResultat> utledAksjonspunkterFor(AksjonspunktUtlederInput param) {

        if (harBrukerAnnenSakForSammeBarn(param) == JA) {
            return AksjonspunktUtlederResultat.opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);
        }

        if (harAnnenpartSakForSammeBarn(param) == JA) {
            return AksjonspunktUtlederResultat.opprettListeForAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE);
        }

        return INGEN_AKSJONSPUNKTER;
    }

    private Utfall harBrukerAnnenSakForSammeBarn(AksjonspunktUtlederInput param) {
        var annenSakSammeBarn = ytelseTjeneste.harAktørAnnenSakMedSammeFamilieHendelse(param.getSaksnummer(), param.getBehandlingId(), param.getAktørId());
        return annenSakSammeBarn ? JA : NEI;
    }

    private Utfall harAnnenpartSakForSammeBarn(AksjonspunktUtlederInput param) {
        var annenSakSammeBarn = finnOppgittAnnenPart(param.getBehandlingId())
            .filter(aktørId -> ytelseTjeneste.harAktørAnnenSakMedSammeFamilieHendelse(param.getSaksnummer(), param.getBehandlingId(), aktørId))
            .isPresent();
        return annenSakSammeBarn ? JA : NEI;
    }

    private Optional<AktørId> finnOppgittAnnenPart(Long behandlingId) {
        return personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandlingId)
            .flatMap(PersonopplysningGrunnlagEntitet::getOppgittAnnenPart)
            .map(OppgittAnnenPartEntitet::getAktørId);
    }


}
