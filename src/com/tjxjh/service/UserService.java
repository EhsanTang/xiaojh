package com.tjxjh.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts2.ServletActionContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cn.cafebabe.websupport.service.BaseService;

import com.tjxjh.action.UserAction;
import com.tjxjh.enumeration.UserStatus;
import com.tjxjh.po.Activity;
import com.tjxjh.po.Club;
import com.tjxjh.po.Merchant;
import com.tjxjh.po.User;
import com.tjxjh.util.CodeUtil;
import com.tjxjh.util.FileUtil;
import com.tjxjh.util.ImageCutAndZoom;

@Service
public class UserService extends BaseService
{
	public static final String DEFAULT_PORTRAIT = "auto_photo.png";
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean register(User user, File portrait)
	{
		user.setStatus(UserStatus.NO_VALIDATE);
		if(portrait != null)
		{
			savePortrait(user.getPortraitPath(), portrait, 280);
		}
		else
		{
			user.setPortraitPath(replaceToDefaultPortrait(user
					.getPortraitPath()));
		}
		return super.save(md5Password(user));
	}
	@Transactional(propagation = Propagation.REQUIRED)
	public User findById(Integer id)
	{
		return dao.findById(User.class, id);
	}
	
	private String replaceToDefaultPortrait(String portraitPath)
	{
		return UserAction.PORTRAIT_FOLDER + DEFAULT_PORTRAIT;
	}
	
	static void savePortrait(String basePath, File file, int width)
	{
		String imgPath = ServletActionContext.getServletContext().getRealPath(
				"/")
				+ basePath;
		FileUtil.copyFileToPath(file, imgPath);
		ImageCutAndZoom.zoom(imgPath, width);
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean update(User user, File portrait)
	{
		if(portrait != null)
		{
			savePortrait(user.getPortraitPath(), portrait, 280);
		}
		return super.update(user, "id") != 0;
	}
	
	public User refresh(User user)
	{
		if(user != null && user.getId() != null)
		{
			return dao.refresh(user);
		}
		else
		{
			return null;
		}
	}
	
	public User login(User user, UserStatus status)
	{
		try
		{
			Assert.notNull(user.getName(), "用户登陆名不能为空");
			user.setStatus(status);
			return super.login(md5Password(user));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public List<User> allUsers()
	{
		return dao.findAll(User.class);
	}
	
	public boolean isRegister(String name)
	{
		return (Long) dao.executeHql("select count(*) from User where name=?",
				name).get(0) > 0l;
	}
	
	public void deleteOldPortraitPath(String oldPortraitPath)
	{
		if(oldPortraitPath != null && !oldPortraitPath.trim().equals(""))
		{
			FileUtil.delete("/" + oldPortraitPath);
		}
	}
	
	static User md5Password(User user)
	{
		user.setPassword(CodeUtil.md5(user.getPassword()));
		return user;
	}
	
	public Map<Integer, User> getFocusUsers(User user)
	{
		Map<Integer, User> userMap = new HashMap<Integer, User>();
		User persistUser = dao.findById(User.class, user.getId());
		Set<User> focusSet = persistUser.getUsersForTargetUserId();
		Iterator<User> it = focusSet.iterator();
		while(it.hasNext())
		{
			User u = it.next();
			userMap.put(u.getId(), u);
		}
		return userMap;
	}
	
	public List<User> getFocusUserList(User user)
	{
		List<User> userList = new ArrayList<User>();
		User persistUser = dao.findByExample(user).get(0);
		Set<User> focusSet = persistUser.getUsersForTargetUserId();
		Iterator<User> it = focusSet.iterator();
		while(it.hasNext())
		{
			User u = it.next();
			userList.add(u);
		}
		return userList;
	}
	
	public <T> List<T> getFocusList(Class objectClass, User user)
	{
		List<T> list = new ArrayList<T>();
		User p = dao.findById(User.class, user.getId());
		Set<T> focusSet = null;
		if(objectClass == User.class)
		{
			focusSet = (Set<T>) p.getUsersForTargetUserId();
		}
		else if(objectClass == Club.class)
		{
			focusSet = (Set<T>) p.getFocusClubs();
		}
		else if(objectClass == Merchant.class)
		{
			focusSet = (Set<T>) p.getMerchants();
		}
		Iterator<T> it = focusSet.iterator();
		while(it.hasNext())
		{
			list.add(it.next());
		}
		return list;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean focusUser(User sourceUser, User targetUser)
	{
		try
		{
			if(!exist(targetUser))
			{
				throw new Exception();
			}
			else
			{
				User user = dao.findById(User.class, targetUser.getId());
				refresh(sourceUser).getUsersForTargetUserId()
						.add(refresh(user));
				dao.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean cancelFocusUser(User sourceUser, User targetUser)
	{
		try
		{
			if(!exist(targetUser))
			{
				throw new Exception();
			}
			else
			{
				User user = dao.findById(User.class, targetUser.getId());
				refresh(sourceUser).getUsersForTargetUserId().remove(user);
				dao.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean focusClub(User sourceUser, Club club)
	{
		try
		{
			if(!exist(club))
			{
				throw new Exception();
			}
			else
			{
				Club c = dao.findById(Club.class, club.getId());
				refresh(sourceUser).getFocusClubs().add(c);
				dao.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean cancelFocusClub(User sourceUser, Club club)
	{
		try
		{
			if(!exist(club))
			{
				throw new Exception();
			}
			else
			{
				Club c = dao.findById(Club.class, club.getId());
				refresh(sourceUser).getFocusClubs().remove(c);
				dao.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean focusMerchant(User sourceUser, Merchant merchant)
	{
		try
		{
			if(!exist(merchant))
			{
				throw new Exception();
			}
			else
			{
				Merchant m = dao.findById(Merchant.class, merchant.getId());
				refresh(sourceUser).getMerchants().add(m);
				dao.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Transactional(propagation = Propagation.REQUIRED)
	public boolean cancelFocusMerchant(User sourceUser, Merchant merchant)
	{
		try
		{
			if(!exist(merchant))
			{
				throw new Exception();
			}
			else
			{
				Merchant m = dao.findById(Merchant.class, merchant.getId());
				refresh(sourceUser).getMerchants().remove(m);
				dao.flush();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
