package japicmp.model;

import com.google.common.base.Optional;
import japicmp.cmp.JarArchiveComparator;
import japicmp.exception.JApiCmpException;
import japicmp.util.MethodDescriptorParser;
import javassist.CtMethod;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import java.util.logging.Logger;

public class JApiMethod extends JApiBehavior {
    private final Optional<CtMethod> oldMethod;
    private final Optional<CtMethod> newMethod;
    private final JApiReturnType returnType;
    private JarArchiveComparator jarArchiveComparator;

    public JApiMethod(String name, JApiChangeStatus changeStatus, Optional<CtMethod> oldMethod, Optional<CtMethod> newMethod) {
    	super(name, oldMethod, newMethod, changeStatus);
        this.oldMethod = oldMethod;
        this.newMethod = newMethod;
        this.returnType = computeReturnTypeChanges(oldMethod, newMethod);
        this.changeStatus = evaluateChangeStatus(this.changeStatus);
    }

    public JApiMethod(JarArchiveComparator jarArchiveComparator, String name, JApiChangeStatus changeStatus, Optional<CtMethod> oldMethod, Optional<CtMethod> newMethod) {
      super(jarArchiveComparator, name, oldMethod, newMethod, changeStatus);
      this.jarArchiveComparator = jarArchiveComparator;
      this.oldMethod = oldMethod;
      this.newMethod = newMethod;
      this.returnType = computeReturnTypeChanges(oldMethod, newMethod);
      this.changeStatus = evaluateChangeStatus(this.changeStatus);
    }

    private JApiChangeStatus evaluateChangeStatus(JApiChangeStatus changeStatus) {
        if (changeStatus == JApiChangeStatus.UNCHANGED) {
            JApiModifier<BridgeModifier> bridgeModifier = getBridgeModifier();

            if ((bridgeModifier.getOldModifier().get() != bridgeModifier.getNewModifier().get() && (
                jarArchiveComparator == null || !jarArchiveComparator.getJarArchiveComparatorOptions().isIgnoreBridge()))) {
                if (this.returnType.getChangeStatus() != JApiChangeStatus.UNCHANGED) {
                  changeStatus = JApiChangeStatus.MODIFIED;
                }
            }
        }
        return changeStatus;
    }

    private JApiReturnType computeReturnTypeChanges(Optional<CtMethod> oldMethodOptional, Optional<CtMethod> newMethodOptional) {
        JApiReturnType jApiReturnType = new JApiReturnType(JApiChangeStatus.UNCHANGED, Optional.<String>absent(), Optional.<String>absent());

        JApiChangeStatus changeStatusReturnType = JApiChangeStatus.UNCHANGED;
        if(oldMethodOptional.isPresent() && newMethodOptional.isPresent()) {
            String oldReturnType = computeReturnType(oldMethodOptional.get());
            String newReturnType = computeReturnType(newMethodOptional.get());
            JApiModifier<BridgeModifier> bridgeModifier = this.getBridgeModifier();

            if ((bridgeModifier.getOldModifier().get() != bridgeModifier.getNewModifier().get())) {
              LOGGER.info("NOT EQUAL " + jarArchiveComparator);
            }
            if ((bridgeModifier.getOldModifier().get() != bridgeModifier.getNewModifier().get() && (
              jarArchiveComparator != null && jarArchiveComparator.getJarArchiveComparatorOptions().isIgnoreBridge()))) {


        LOGGER.info("Old " + bridgeModifier.getOldModifier().get() + "  New " + bridgeModifier.getNewModifier().get());
              changeStatusReturnType = JApiChangeStatus.UNCHANGED;
            }
            else if (!oldReturnType.equals(newReturnType)) {
                changeStatusReturnType = JApiChangeStatus.MODIFIED;
            }
            jApiReturnType = new JApiReturnType(changeStatusReturnType, Optional.of(oldReturnType), Optional.of(newReturnType));
        } else {
            if (oldMethodOptional.isPresent()) {
                String oldReturnType = computeReturnType(oldMethodOptional.get());
                jApiReturnType = new JApiReturnType(JApiChangeStatus.REMOVED, Optional.of(oldReturnType), Optional.<String>absent());
            }
            if (newMethodOptional.isPresent()) {
                String newReturnType = computeReturnType(newMethodOptional.get());
                jApiReturnType = new JApiReturnType(JApiChangeStatus.NEW, Optional.<String>absent(), Optional.of(newReturnType));
            }
        }

      if (changeStatusReturnType == JApiChangeStatus.MODIFIED) {
        JApiModifier<BridgeModifier> bridgeModifier = this.getBridgeModifier();
      }

        return jApiReturnType;
    }

    private String computeReturnType(CtMethod oldMethod) {
        MethodDescriptorParser parser = new MethodDescriptorParser();
        parser.parse(oldMethod.getSignature());
        return parser.getReturnType();
    }

    public boolean hasSameReturnType(JApiMethod otherMethod) {
        boolean haveSameReturnType = false;
        JApiReturnType otherReturnType = otherMethod.getReturnType();
        if (otherReturnType.getChangeStatus() == JApiChangeStatus.UNCHANGED || otherReturnType.getChangeStatus() == JApiChangeStatus.MODIFIED) {
            if (this.returnType.getChangeStatus() == JApiChangeStatus.UNCHANGED || this.returnType.getChangeStatus() == JApiChangeStatus.MODIFIED) {
                if (otherReturnType.getOldReturnType().equals(this.returnType.getOldReturnType()) && otherReturnType.getNewReturnType().equals(this.returnType.getNewReturnType())) {
                    haveSameReturnType = true;
                }
            } else if(this.returnType.getChangeStatus() == JApiChangeStatus.NEW) {
                if (otherReturnType.getNewReturnType().equals(this.returnType.getNewReturnType())) {
                    haveSameReturnType = true;
                }
            } else if(this.returnType.getChangeStatus() == JApiChangeStatus.REMOVED) {
                if (otherReturnType.getOldReturnType().equals(this.returnType.getOldReturnType())) {
                    haveSameReturnType = true;
                }
            }
        } else if (otherReturnType.getChangeStatus() == JApiChangeStatus.NEW) {
            if (this.returnType.getChangeStatus() == JApiChangeStatus.UNCHANGED || this.returnType.getChangeStatus() == JApiChangeStatus.MODIFIED) {
                if (otherReturnType.getNewReturnType().equals(this.returnType.getNewReturnType())) {
                    haveSameReturnType = true;
                }
            } else if(this.returnType.getChangeStatus() == JApiChangeStatus.NEW) {
                if (otherReturnType.getNewReturnType().equals(this.returnType.getNewReturnType())) {
                    haveSameReturnType = true;
                }
            } else if(this.returnType.getChangeStatus() == JApiChangeStatus.REMOVED) {
                if (otherReturnType.getNewReturnType().equals(this.returnType.getOldReturnType())) {
                    haveSameReturnType = true;
                }
            }
        } else {
            if (this.returnType.getChangeStatus() == JApiChangeStatus.UNCHANGED || this.returnType.getChangeStatus() == JApiChangeStatus.MODIFIED) {
                if (otherReturnType.getOldReturnType().equals(this.returnType.getNewReturnType())) {
                    haveSameReturnType = true;
                }
            } else if(this.returnType.getChangeStatus() == JApiChangeStatus.NEW) {
                if (otherReturnType.getOldReturnType().equals(this.returnType.getNewReturnType())) {
                    haveSameReturnType = true;
                }
            } else if(this.returnType.getChangeStatus() == JApiChangeStatus.REMOVED) {
                if (otherReturnType.getOldReturnType().equals(this.returnType.getOldReturnType())) {
                    haveSameReturnType = true;
                }
            }
        }

        JApiModifier<BridgeModifier> bridgeModifier = getBridgeModifier();

        return haveSameReturnType;
    }

    @XmlTransient
    public Optional<CtMethod> getNewMethod() {
        return newMethod;
    }

    @XmlTransient
    public Optional<CtMethod> getOldMethod() {
        return oldMethod;
    }

    @XmlElement(name = "returnType")
    public JApiReturnType getReturnType() {
        return returnType;
    }

  private static final Logger LOGGER = Logger.getLogger(JarArchiveComparator.class.getName());
}
